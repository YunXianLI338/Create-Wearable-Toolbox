package com.minecart.wearabletoolbox.common;

import com.minecart.wearabletoolbox.mixin.ToolboxInventoryAccess;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import com.simibubi.create.foundation.networking.ISyncPersistentData;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.UUID;

/**
 * 可穿戴工具箱的快捷栏绑定与自动补充。
 *
 * <p>Create 的放置版工具箱用方块坐标把“某个隔间”绑定到快捷栏，并在
 * {@code ToolboxBlockEntity#tickPlayers} 中自动补充/回收快捷栏物品。可穿戴版没有方块，
 * 因此把绑定数据放在玩家持久化 NBT 里，并每 tick 按同样的规则维护。</p>
 */
public final class WearableToolboxBindings {
    /** 玩家持久化 NBT 中的根键。 */
    public static final String DATA_KEY = "WearableToolboxData";
    private static final String TOOLBOX_KEY = "Toolbox";
    private static final String SLOT_KEY = "Slot";

    private WearableToolboxBindings() {
    }

    private static CompoundTag root(ServerPlayer player) {
        return player.getPersistentData().getCompound(DATA_KEY);
    }

    /**
     * 当前快捷栏槽位是否已绑定隔间。客户端的持久化数据由 Create 的 syncData 同步，
     * 因此这里可以安全地在客户端读取。
     */
    public static boolean hasBinding(Player player, int hotbarSlot) {
        return player.getPersistentData()
                .getCompound(DATA_KEY)
                .contains(String.valueOf(hotbarSlot));
    }

    private static void saveRoot(ServerPlayer player, CompoundTag root) {
        if (root.isEmpty()) {
            player.getPersistentData().remove(DATA_KEY);
        } else {
            player.getPersistentData().put(DATA_KEY, root);
        }
    }

    /**
     * 同步玩家持久化数据。
     *
     * <p>不能直接用 Create 的 {@code ToolboxHandler.syncData}：它把玩家正在使用的
     * {@code CompoundTag} 原样塞进网络包，而网络编码在 Netty 线程上异步执行。
     * 绑定/补充逻辑随后还会继续修改同一棵树，编码时就会抛出
     * {@code ConcurrentModificationException} 并断开客户端。这里发送一份深拷贝。</p>
     */
    public static void syncData(ServerPlayer player) {
        CatnipServices.NETWORK.sendToClient(
                player,
                new ISyncPersistentData.PersistentDataPacket(player.getId(), player.getPersistentData().copy())
        );
    }

    /**
     * 把服务端刚写回装备槽的工具箱物品发给客户端。
     *
     * <p>胸甲栏的物品组件不一定立刻同步给玩家自己，径向菜单读取本地物品栈时会看到旧库存，
     * 导致“隔间已空但图标仍是亮的”。</p>
     */
    public static void syncToolbox(ServerPlayer player, int curioSlot, ItemStack stack) {
        PacketDistributor.sendToPlayer(player, new SyncToolboxPayload(stack.copy(), curioSlot));
    }

    /**
     * 手动“全部存入”前解除所有快捷栏绑定，但不像 {@link #unbind} 那样把快捷栏物品搬回去。
     *
     * <p>Create 的 {@code ToolboxDisposeAllPacket} 也会先 unequip 指向该工具箱的快捷栏，
     * 否则存进去的物品会在下一次 tick 被自动补充逻辑重新拿回快捷栏。</p>
     */
    public static void clearForDeposit(ServerPlayer player) {
        CompoundTag root = root(player);
        if (root.isEmpty()) {
            return;
        }

        boolean changed = false;
        for (String key : List.copyOf(root.getAllKeys())) {
            if (key.length() == 1 && key.charAt(0) >= '0' && key.charAt(0) <= '8') {
                root.remove(key);
                changed = true;
            }
        }
        if (!changed) {
            return;
        }

        boolean hasBindings = root.getAllKeys().stream()
                .anyMatch(key -> key.length() == 1 && key.charAt(0) >= '0' && key.charAt(0) <= '8');
        if (!hasBindings) {
            player.getPersistentData().remove(DATA_KEY);
        } else {
            saveRoot(player, root);
        }
        syncData(player);
    }

    /**
     * 绑定一个隔间到快捷栏。与 Create 一致：不立即搬动当前物品，而是在 tick 中补充/回收。
     */
    public static void bind(ServerPlayer player, int hotbarSlot, int compartment) {
        if (hotbarSlot < 0 || hotbarSlot >= Inventory.getSelectionSize() || compartment < 0 || compartment >= 8) {
            return;
        }

        EquippedToolbox equipped = EquippedToolbox.find(player);
        if (equipped == null) {
            return;
        }

        ToolboxInventory inventory = equipped.stack().get(AllDataComponents.TOOLBOX_INVENTORY);
        if (inventory == null) {
            inventory = new ToolboxInventory(null);
        }
        List<ItemStack> filters = filters(inventory);
        if (compartment >= filters.size() || filters.get(compartment).isEmpty()) {
            return;
        }
        // 过滤器只是代表物，隔间里可能已经空了（物品正被拿在快捷栏里）。
        // 此时再绑定会先解掉旧绑定，然后从空隔间里“取出空气”。
        if (!hasCompartmentItems(inventory, compartment)) {
            return;
        }

        // 同一快捷栏已有绑定：先把旧隔间的物品收回，再改绑。
        unbind(player, hotbarSlot);

        equipped = EquippedToolbox.find(player);
        if (equipped == null) {
            return;
        }
        ItemStack toolbox = equipped.stack();
        inventory = toolbox.get(AllDataComponents.TOOLBOX_INVENTORY);
        if (inventory == null) {
            inventory = new ToolboxInventory(null);
        }
        filters = filters(inventory);

        UUID id = toolbox.get(AllDataComponents.TOOLBOX_UUID);
        if (id == null) {
            id = UUID.randomUUID();
            toolbox.set(AllDataComponents.TOOLBOX_UUID, id);
        }

        CompoundTag root = root(player);
        root.putUUID(TOOLBOX_KEY, id);
        CompoundTag entry = new CompoundTag();
        entry.putInt(SLOT_KEY, compartment);
        root.put(String.valueOf(hotbarSlot), entry);
        saveRoot(player, root);
        // 快捷栏边框由客户端读取持久化数据绘制，绑定变化后立即同步。
        syncData(player);

        EquippedToolbox.set(player, equipped.curioSlot(), toolbox);
        syncToolbox(player, equipped.curioSlot(), toolbox);
        player.displayClientMessage(
                Component.translatable("message.wearable_toolbox.bound", filters.get(compartment).getHoverName())
                        .withStyle(ChatFormatting.GREEN),
                true
        );
    }

    /**
     * 解除快捷栏绑定，并把该快捷栏里的同类物品收回工具箱。
     */
    public static void unbind(ServerPlayer player, int hotbarSlot) {
        if (hotbarSlot < 0 || hotbarSlot >= Inventory.getSelectionSize()) {
            return;
        }

        CompoundTag root = root(player);
        String key = String.valueOf(hotbarSlot);
        if (!root.contains(key)) {
            return;
        }

        int compartment = root.getCompound(key).getInt(SLOT_KEY);
        root.remove(key);

        EquippedToolbox equipped = EquippedToolbox.find(player);
        if (equipped != null) {
            ToolboxInventory inventory = equipped.stack().get(AllDataComponents.TOOLBOX_INVENTORY);
            if (inventory != null && compartment >= 0 && compartment < 8) {
                Inventory playerInventory = player.getInventory();
                ItemStack current = playerInventory.getItem(hotbarSlot);
                if (!current.isEmpty()) {
                    ItemStack toInsert = ToolboxInventory.cleanItemNBT(current.copy());
                    ItemStack remainder = inventory.distributeToCompartment(toInsert, compartment, false);
                    if (remainder.getCount() != toInsert.getCount()) {
                        playerInventory.setItem(hotbarSlot, remainder);
                    }
                }

                ItemStack updated = equipped.stack().copy();
                updated.set(AllDataComponents.TOOLBOX_INVENTORY, inventory);
                EquippedToolbox.set(player, equipped.curioSlot(), updated);
                syncToolbox(player, equipped.curioSlot(), updated);
            }
        }

        saveRoot(player, root);
        syncData(player);
        player.displayClientMessage(
                Component.translatable("message.wearable_toolbox.unbound").withStyle(ChatFormatting.GRAY),
                true
        );
    }

    /**
     * 每 tick 由服务端调用：校验绑定并按 Create 的规则补充/回收快捷栏物品。
     */
    public static void tick(ServerPlayer player) {
        CompoundTag root = root(player);
        if (root.isEmpty()) {
            return;
        }

        EquippedToolbox equipped = EquippedToolbox.find(player);
        if (equipped == null) {
            player.getPersistentData().remove(DATA_KEY);
            syncData(player);
            return;
        }

        ItemStack toolbox = equipped.stack();
        UUID id = toolbox.get(AllDataComponents.TOOLBOX_UUID);
        if (id == null || !id.equals(root.getUUID(TOOLBOX_KEY))) {
            // 装备槽换成了另一个工具箱（或物品丢失了 UUID），旧绑定失效。
            player.getPersistentData().remove(DATA_KEY);
            syncData(player);
            return;
        }

        ToolboxInventory inventory = toolbox.get(AllDataComponents.TOOLBOX_INVENTORY);
        if (inventory == null) {
            return;
        }

        // 界面打开时由菜单处理槽位；避免同一 tick 内两边同时搬动物品。
        if (player.containerMenu instanceof WearableToolboxMenu) {
            return;
        }

        List<ItemStack> filters = filters(inventory);
        Inventory playerInventory = player.getInventory();
        boolean inventoryChanged = false;
        boolean rootChanged = false;

        for (int hotbarSlot = 0; hotbarSlot < Inventory.getSelectionSize(); hotbarSlot++) {
            String key = String.valueOf(hotbarSlot);
            if (!root.contains(key)) {
                continue;
            }

            int compartment = root.getCompound(key).getInt(SLOT_KEY);
            if (compartment < 0 || compartment >= 8 || compartment >= filters.size()) {
                root.remove(key);
                rootChanged = true;
                continue;
            }

            ItemStack filter = filters.get(compartment);
            if (filter.isEmpty()) {
                root.remove(key);
                rootChanged = true;
                continue;
            }

            ItemStack current = playerInventory.getItem(hotbarSlot);
            if (!current.isEmpty() && !ToolboxInventory.canItemsShareCompartment(current, filter)) {
                root.remove(key);
                rootChanged = true;
                continue;
            }

            int target = (filter.getMaxStackSize() + 1) / 2;
            int count = current.getCount();
            if (count < target) {
                ItemStack extracted = inventory.takeFromCompartment(target - count, compartment, false);
                if (!extracted.isEmpty()) {
                    ItemStack template = current.isEmpty() ? extracted : current;
                    playerInventory.setItem(hotbarSlot, template.copyWithCount(count + extracted.getCount()));
                    inventoryChanged = true;
                }
            } else if (count > target) {
                int amount = count - target;
                ItemStack toInsert = current.copyWithCount(amount);
                int deposited = amount - inventory.distributeToCompartment(toInsert, compartment, false).getCount();
                if (deposited > 0) {
                    playerInventory.setItem(hotbarSlot, current.copyWithCount(count - deposited));
                    inventoryChanged = true;
                }
            }
        }

        if (rootChanged) {
            saveRoot(player, root);
            syncData(player);
        }
        if (inventoryChanged) {
            ItemStack updated = toolbox.copy();
            updated.set(AllDataComponents.TOOLBOX_INVENTORY, inventory);
            EquippedToolbox.set(player, equipped.curioSlot(), updated);
            syncToolbox(player, equipped.curioSlot(), updated);
            playerInventory.setChanged();
        }
    }

    private static List<ItemStack> filters(ToolboxInventory inventory) {
        return ((ToolboxInventoryAccess) (Object) inventory).wearableToolbox$filters();
    }

    /**
     * 隔间里是否真的有物品（而不是只有过滤器代表物）。
     */
    public static boolean hasCompartmentItems(ToolboxInventory inventory, int compartment) {
        if (compartment < 0 || compartment >= 8) {
            return false;
        }
        for (int i = 0; i < ToolboxInventory.STACKS_PER_COMPARTMENT; i++) {
            if (!inventory.getStackInSlot(compartment * ToolboxInventory.STACKS_PER_COMPARTMENT + i).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
