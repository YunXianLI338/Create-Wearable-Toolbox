package com.minecart.wearabletoolbox.common;

import com.minecart.wearabletoolbox.mixin.ToolboxInventoryAccess;
import com.simibubi.create.content.equipment.toolbox.ItemReturnInvWrapper;
import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 可穿戴工具箱的快捷栏绑定与自动补充。
 *
 * <p>Create 的放置版工具箱用方块坐标把“某个隔间”绑定到快捷栏，并在
 * {@code ToolboxBlockEntity#tickPlayers} 中自动补充/回收快捷栏物品。可穿戴版没有方块，
 * 因此把绑定数据放在玩家持久化 NBT 里，并每 tick 按同样的规则维护。</p>
 *
 * <p>1.20.1 与 1.21.1 的差别：工具箱库存不再是物品数据组件，而是物品 NBT 的
 * {@code Inventory} 子标签，所以每次修改都要显式读出来、改完再写回去。</p>
 */
public final class WearableToolboxBindings {
    /** 玩家持久化 NBT 中的根键。 */
    public static final String DATA_KEY = "WearableToolboxData";
    private static final String TOOLBOX_KEY = "Toolbox";
    private static final String SLOT_KEY = "Slot";
    private static final String CURIO_KEY = "Curio";
    private static final String INVENTORY_KEY = "Inventory";

    private WearableToolboxBindings() {
    }

    private static UUID entryUuid(CompoundTag root, String key) {
        CompoundTag entry = root.getCompound(key);
        return entry.contains(TOOLBOX_KEY) ? entry.getUUID(TOOLBOX_KEY) : root.getUUID(TOOLBOX_KEY);
    }

    private static int entryCurio(CompoundTag root, String key) {
        CompoundTag entry = root.getCompound(key);
        return entry.contains(CURIO_KEY) ? entry.getInt(CURIO_KEY) : -1;
    }

    private static int entryInventory(CompoundTag root, String key) {
        CompoundTag entry = root.getCompound(key);
        return entry.contains(INVENTORY_KEY) ? entry.getInt(INVENTORY_KEY) : -1;
    }

    private static CompoundTag root(ServerPlayer player) {
        return player.getPersistentData().getCompound(DATA_KEY);
    }

    /**
     * 当前快捷栏槽位是否已绑定隔间。客户端的持久化数据由 {@link #syncData} 同步，
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
     * 同步玩家持久化数据中的绑定部分。
     *
     * <p>只能发深拷贝：网络编码在 Netty 线程上异步执行，而绑定/补充逻辑随后还会继续
     * 修改同一棵树，直接发引用会抛 {@code ConcurrentModificationException} 并断开客户端。</p>
     */
    public static void syncData(ServerPlayer player) {
        WearableToolboxNetwork.sendToPlayer(player, new SyncBindingsPayload(root(player).copy()));
    }

    /**
     * 把服务端刚写回装备槽的工具箱物品发给客户端。
     *
     * <p>胸甲栏的物品数据不一定立刻同步给玩家自己，径向菜单读取本地物品栈时会看到旧库存，
     * 导致“隔间已空但图标仍是亮的”。</p>
     */
    public static void syncToolbox(ServerPlayer player, int curioSlot, int inventorySlot, ItemStack stack) {
        WearableToolboxNetwork.sendToPlayer(player, new SyncToolboxPayload(stack.copy(), curioSlot, inventorySlot));
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
     * 绑定一个隔间到快捷栏。手上物品与目标过滤器不匹配时先收进工具箱，之后由 tick 补充/回收。
     */
    public static void bind(ServerPlayer player, int hotbarSlot, int compartment, int curioSlot, int inventorySlot) {
        if (hotbarSlot < 0 || hotbarSlot >= Inventory.getSelectionSize() || compartment < 0 || compartment >= 8) {
            return;
        }

        EquippedToolbox equipped = EquippedToolbox.find(player, curioSlot, inventorySlot);
        if (equipped == null) {
            return;
        }

        ToolboxInventory inventory = ToolboxStorage.readInventory(equipped.stack());
        List<ItemStack> filters = filters(inventory);
        if (compartment >= filters.size() || filters.get(compartment).isEmpty()) {
            return;
        }
        // 过滤器只是代表物，隔间里可能已经空了（物品正被拿在快捷栏里）。
        // 此时再绑定会先解掉旧绑定，然后从空隔间里“取出空气”。
        // 若快捷栏里已经是同类物品，则本次绑定仍然有意义（后续 tick 会维持数量）。
        ItemStack handBeforeUnbind = player.getInventory().getItem(hotbarSlot);
        if (!hasCompartmentItems(inventory, compartment)
                && !ToolboxInventory.canItemsShareCompartment(handBeforeUnbind, filters.get(compartment))) {
            return;
        }

        // 与 Create 的 ToolboxEquipPacket 一致：先解除该快捷栏的旧绑定，再处理手上物品。
        unbind(player, hotbarSlot, false);

        equipped = EquippedToolbox.find(player, curioSlot, inventorySlot);
        if (equipped == null) {
            return;
        }
        ItemStack toolbox = equipped.stack();
        inventory = ToolboxStorage.readInventory(toolbox);
        filters = filters(inventory);

        // 与 Create 的 ToolboxEquipPacket 一致：手上拿着不属于目标隔间的物品时，
        // 先把它收进工具箱，否则下一次 tick 会因为“快捷栏物品与过滤器不匹配”直接解绑。
        ItemStack hand = player.getInventory().getItem(hotbarSlot);
        ItemStack filter = filters.get(compartment);
        if (!hand.isEmpty() && !ToolboxInventory.canItemsShareCompartment(hand, filter)) {
            ItemStack toInsert = hand.copy();
            ItemStack[] remainder = {toInsert};
            inventory.inLimitedMode(inv ->
                    remainder[0] = ItemHandlerHelper.insertItemStacked(inv, toInsert, false)
            );
            if (!remainder[0].isEmpty()) {
                remainder[0] = ItemHandlerHelper.insertItemStacked(
                        new ItemReturnInvWrapper(player.getInventory()), remainder[0], false
                );
            }
            if (remainder[0].getCount() != toInsert.getCount()) {
                player.getInventory().setItem(hotbarSlot, remainder[0]);
                player.getInventory().setChanged();
                ToolboxStorage.writeInventory(toolbox, inventory);
                ToolboxStorage.ensureId(toolbox);
                EquippedToolbox.set(player, equipped.curioSlot(), equipped.inventorySlot(), toolbox);
                syncToolbox(player, equipped.curioSlot(), equipped.inventorySlot(), toolbox);
            }
        }

        UUID id = ToolboxStorage.ensureId(toolbox);

        CompoundTag root = root(player);
        root.putUUID(TOOLBOX_KEY, id);
        CompoundTag entry = new CompoundTag();
        entry.putUUID(TOOLBOX_KEY, id);
        entry.putInt(SLOT_KEY, compartment);
        entry.putInt(CURIO_KEY, equipped.curioSlot());
        entry.putInt(INVENTORY_KEY, equipped.inventorySlot());
        root.put(String.valueOf(hotbarSlot), entry);
        saveRoot(player, root);
        // 快捷栏边框由客户端读取持久化数据绘制，绑定变化后立即同步。
        syncData(player);

        EquippedToolbox.set(player, equipped.curioSlot(), equipped.inventorySlot(), toolbox);
        syncToolbox(player, equipped.curioSlot(), equipped.inventorySlot(), toolbox);
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
        unbind(player, hotbarSlot, true);
    }

    /**
     * @param announce 是否提示玩家；重新绑定时只做静默解绑。
     */
    public static void unbind(ServerPlayer player, int hotbarSlot, boolean announce) {
        if (hotbarSlot < 0 || hotbarSlot >= Inventory.getSelectionSize()) {
            return;
        }

        CompoundTag root = root(player);
        String key = String.valueOf(hotbarSlot);
        if (!root.contains(key)) {
            return;
        }

        int compartment = root.getCompound(key).getInt(SLOT_KEY);
        int curioSlot = entryCurio(root, key);
        int inventorySlot = entryInventory(root, key);
        UUID boundId = entryUuid(root, key);
        root.remove(key);

        EquippedToolbox equipped = EquippedToolbox.find(player, curioSlot, inventorySlot);
        if (equipped != null && boundId != null
                && boundId.equals(ToolboxStorage.ensureId(equipped.stack()))) {
            ItemStack toolbox = equipped.stack();
            ToolboxInventory inventory = ToolboxStorage.readInventory(toolbox);
            if (compartment >= 0 && compartment < 8) {
                Inventory playerInventory = player.getInventory();
                ItemStack current = playerInventory.getItem(hotbarSlot);
                if (!current.isEmpty()) {
                    ItemStack toInsert = ToolboxInventory.cleanItemNBT(current.copy());
                    ItemStack remainder = inventory.distributeToCompartment(toInsert, compartment, false);
                    if (remainder.getCount() != toInsert.getCount()) {
                        playerInventory.setItem(hotbarSlot, remainder);
                    }
                }

                ToolboxStorage.writeInventory(toolbox, inventory);
                ToolboxStorage.ensureId(toolbox);
                EquippedToolbox.set(player, equipped.curioSlot(), equipped.inventorySlot(), toolbox);
                syncToolbox(player, equipped.curioSlot(), equipped.inventorySlot(), toolbox);
            }
        }

        saveRoot(player, root);
        syncData(player);
        if (announce) {
            player.displayClientMessage(
                    Component.translatable("message.wearable_toolbox.unbound").withStyle(ChatFormatting.GRAY),
                    true
            );
        }
    }

    /**
     * 每 tick 由服务端调用：按每个快捷栏自己的来源工具箱校验并按 Create 的规则补充/回收。
     */
    public static void tick(ServerPlayer player) {
        CompoundTag root = root(player);
        if (root.isEmpty()) {
            return;
        }

        // 界面打开时由菜单处理槽位；避免同一 tick 内两边同时搬动物品。
        if (player.containerMenu instanceof WearableToolboxMenu) {
            return;
        }

        Inventory playerInventory = player.getInventory();
        boolean rootChanged = false;

        for (int hotbarSlot = 0; hotbarSlot < Inventory.getSelectionSize(); hotbarSlot++) {
            String key = String.valueOf(hotbarSlot);
            if (!root.contains(key)) {
                continue;
            }

            int compartment = root.getCompound(key).getInt(SLOT_KEY);
            int curioSlot = entryCurio(root, key);
            int inventorySlot = entryInventory(root, key);
            UUID boundId = entryUuid(root, key);
            if (compartment < 0 || compartment >= 8 || boundId == null) {
                root.remove(key);
                rootChanged = true;
                continue;
            }

            EquippedToolbox equipped = EquippedToolbox.find(player, curioSlot, inventorySlot);
            if (equipped == null || !boundId.equals(ToolboxStorage.ensureId(equipped.stack()))) {
                root.remove(key);
                rootChanged = true;
                continue;
            }

            ItemStack toolbox = equipped.stack();
            ToolboxInventory inventory = ToolboxStorage.readInventory(toolbox);
            List<ItemStack> filters = filters(inventory);
            if (compartment >= filters.size()) {
                root.remove(key);
                rootChanged = true;
                continue;
            }

            ItemStack filter = filters.get(compartment);
            if (filter.isEmpty()) {
                // 与 Create 的 tickPlayers 一致：只解除绑定，不把快捷栏物品强行塞回去。
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

            boolean changed = false;
            int target = (filter.getMaxStackSize() + 1) / 2;
            int count = current.getCount();
            if (count < target) {
                ItemStack extracted = inventory.takeFromCompartment(target - count, compartment, false);
                if (!extracted.isEmpty()) {
                    ItemStack template = current.isEmpty() ? extracted : current;
                    playerInventory.setItem(hotbarSlot, template.copyWithCount(count + extracted.getCount()));
                    changed = true;
                }
            } else if (count > target) {
                int amount = count - target;
                ItemStack toInsert = current.copyWithCount(amount);
                int deposited = amount - inventory.distributeToCompartment(toInsert, compartment, false).getCount();
                if (deposited > 0) {
                    playerInventory.setItem(hotbarSlot, current.copyWithCount(count - deposited));
                    changed = true;
                }
            }

            if (changed) {
                ToolboxStorage.writeInventory(toolbox, inventory);
                ToolboxStorage.ensureId(toolbox);
                EquippedToolbox.set(player, curioSlot, inventorySlot, toolbox);
                syncToolbox(player, curioSlot, inventorySlot, toolbox);
                playerInventory.setChanged();
            }
        }

        if (rootChanged) {
            saveRoot(player, root);
            syncData(player);
        }
    }

    public static Map<Integer, Integer> snapshotBoundCounts(ServerPlayer player, UUID toolboxId, int curioSlot, int inventorySlot, ToolboxInventory inventory) {
        Map<Integer, Integer> counts = new HashMap<>();
        CompoundTag root = root(player);
        for (int hotbarSlot = 0; hotbarSlot < Inventory.getSelectionSize(); hotbarSlot++) {
            String key = String.valueOf(hotbarSlot);
            if (!root.contains(key) || !toolboxId.equals(entryUuid(root, key))) {
                continue;
            }
            if (entryCurio(root, key) != curioSlot || entryInventory(root, key) != inventorySlot) {
                continue;
            }
            int compartment = root.getCompound(key).getInt(SLOT_KEY);
            counts.put(hotbarSlot, countCompartment(inventory, compartment));
        }
        return counts;
    }

    public static void dropManualDeposits(ServerPlayer player, UUID toolboxId, int curioSlot, int inventorySlot,
                                          ToolboxInventory inventory, Map<Integer, Integer> before) {
        if (before.isEmpty()) {
            return;
        }
        CompoundTag root = root(player);
        boolean changed = false;
        for (Map.Entry<Integer, Integer> e : before.entrySet()) {
            String key = String.valueOf(e.getKey());
            if (!root.contains(key) || !toolboxId.equals(entryUuid(root, key))) {
                continue;
            }
            if (entryCurio(root, key) != curioSlot || entryInventory(root, key) != inventorySlot) {
                continue;
            }
            int compartment = root.getCompound(key).getInt(SLOT_KEY);
            if (countCompartment(inventory, compartment) > e.getValue()) {
                root.remove(key);
                changed = true;
            }
        }
        if (changed) {
            saveRoot(player, root);
            syncData(player);
        }
    }

    private static int countCompartment(ToolboxInventory inventory, int compartment) {
        if (compartment < 0 || compartment >= 8) {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < ToolboxInventory.STACKS_PER_COMPARTMENT; i++) {
            count += inventory.getStackInSlot(compartment * ToolboxInventory.STACKS_PER_COMPARTMENT + i).getCount();
        }
        return count;
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
