package com.minecart.wearabletoolbox.common;

import com.minecart.wearabletoolbox.mixin.ToolboxInventoryAccess;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.equipment.toolbox.ToolboxInventory;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

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

    private static void saveRoot(ServerPlayer player, CompoundTag root) {
        if (root.isEmpty()) {
            player.getPersistentData().remove(DATA_KEY);
        } else {
            player.getPersistentData().put(DATA_KEY, root);
        }
    }

    /**
     * 绑定一个隔间到快捷栏。与 Create 一致：不立即搬动当前物品，而是在 tick 中补充/回收。
     */
    public static void bind(ServerPlayer player, int hotbarSlot, int compartment) {
        if (hotbarSlot < 0 || hotbarSlot >= Inventory.getSelectionSize() || compartment < 0 || compartment >= 8) {
            return;
        }

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!ToolboxItems.isToolbox(chest)) {
            return;
        }

        ToolboxInventory inventory = chest.get(AllDataComponents.TOOLBOX_INVENTORY);
        if (inventory == null) {
            inventory = new ToolboxInventory(null);
        }
        List<ItemStack> filters = filters(inventory);
        if (compartment >= filters.size() || filters.get(compartment).isEmpty()) {
            return;
        }

        // 同一快捷栏已有绑定：先把旧隔间的物品收回，再改绑。
        unbind(player, hotbarSlot);

        chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!ToolboxItems.isToolbox(chest)) {
            return;
        }
        inventory = chest.get(AllDataComponents.TOOLBOX_INVENTORY);
        if (inventory == null) {
            inventory = new ToolboxInventory(null);
        }

        UUID id = chest.get(AllDataComponents.TOOLBOX_UUID);
        if (id == null) {
            id = UUID.randomUUID();
            chest.set(AllDataComponents.TOOLBOX_UUID, id);
        }

        CompoundTag root = root(player);
        root.putUUID(TOOLBOX_KEY, id);
        CompoundTag entry = new CompoundTag();
        entry.putInt(SLOT_KEY, compartment);
        root.put(String.valueOf(hotbarSlot), entry);
        saveRoot(player, root);

        player.setItemSlot(EquipmentSlot.CHEST, chest);
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

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (ToolboxItems.isToolbox(chest)) {
            ToolboxInventory inventory = chest.get(AllDataComponents.TOOLBOX_INVENTORY);
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

                ItemStack updated = chest.copy();
                updated.set(AllDataComponents.TOOLBOX_INVENTORY, inventory);
                player.setItemSlot(EquipmentSlot.CHEST, updated);
            }
        }

        saveRoot(player, root);
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

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!ToolboxItems.isToolbox(chest)) {
            player.getPersistentData().remove(DATA_KEY);
            return;
        }

        UUID id = chest.get(AllDataComponents.TOOLBOX_UUID);
        if (id == null || !id.equals(root.getUUID(TOOLBOX_KEY))) {
            // 胸甲栏换成了另一个工具箱（或物品丢失了 UUID），旧绑定失效。
            player.getPersistentData().remove(DATA_KEY);
            return;
        }

        ToolboxInventory inventory = chest.get(AllDataComponents.TOOLBOX_INVENTORY);
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
        }
        if (inventoryChanged) {
            ItemStack updated = chest.copy();
            updated.set(AllDataComponents.TOOLBOX_INVENTORY, inventory);
            player.setItemSlot(EquipmentSlot.CHEST, updated);
            playerInventory.setChanged();
        }
    }

    private static List<ItemStack> filters(ToolboxInventory inventory) {
        return ((ToolboxInventoryAccess) (Object) inventory).wearableToolbox$filters();
    }
}
