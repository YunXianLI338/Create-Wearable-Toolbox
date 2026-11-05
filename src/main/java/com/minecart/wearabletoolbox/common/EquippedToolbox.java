package com.minecart.wearabletoolbox.common;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 玩家身上或物品栏里可打开/绑定的工具箱。
 *
 * @param stack         工具箱物品
 * @param curioSlot     Curios body 槽位下标；-1 表示原版胸甲栏
 * @param inventorySlot 物品栏槽位下标；-1 表示已装备来源
 */
public record EquippedToolbox(ItemStack stack, int curioSlot, int inventorySlot) {
    public static final String CURIO_SLOT = "body";

    public EquippedToolbox(ItemStack stack, int curioSlot) {
        this(stack, curioSlot, -1);
    }

    public boolean isCurio() {
        return !isInventory() && curioSlot >= 0;
    }

    public boolean isInventory() {
        return inventorySlot >= 0;
    }

    public boolean isVisibleOn(Player player) {
        return !isInventory() && (!isCurio() || CuriosCompat.isBodySlotVisible(player, curioSlot));
    }

    @Nullable
    public static EquippedToolbox find(Player player) {
        List<EquippedToolbox> all = findAll(player);
        return all.isEmpty() ? null : all.get(0);
    }

    @Nullable
    public static EquippedToolbox find(Player player, int curioSlot) {
        return find(player, curioSlot, -1);
    }

    @Nullable
    public static EquippedToolbox find(Player player, int curioSlot, int inventorySlot) {
        if (inventorySlot >= 0) {
            if (inventorySlot >= Inventory.INVENTORY_SIZE) {
                return null;
            }
            ItemStack stack = player.getInventory().getItem(inventorySlot);
            return ToolboxItems.isToolbox(stack)
                    ? new EquippedToolbox(stack, -1, inventorySlot)
                    : null;
        }
        if (curioSlot < 0) {
            ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
            return ToolboxItems.isToolbox(chest) ? new EquippedToolbox(chest, -1) : null;
        }
        if (ModList.get().isLoaded("curios")) {
            return CuriosCompat.findBodyAt(player, curioSlot);
        }
        return null;
    }

    public static List<EquippedToolbox> findAll(Player player) {
        List<EquippedToolbox> result = new ArrayList<>();
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (ToolboxItems.isToolbox(chest)) {
            result.add(new EquippedToolbox(chest, -1));
        }
        if (ModList.get().isLoaded("curios")) {
            result.addAll(CuriosCompat.findAllBodies(player));
        }
        return result;
    }

    public static List<EquippedToolbox> findAllIncludingInventory(Player player) {
        List<EquippedToolbox> result = new ArrayList<>(findAll(player));
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < Inventory.INVENTORY_SIZE; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (ToolboxItems.isToolbox(stack)) {
                result.add(new EquippedToolbox(stack, -1, slot));
            }
        }
        return result;
    }

    public static void set(Player player, int curioSlot, ItemStack stack) {
        set(player, curioSlot, -1, stack);
    }

    public static void set(Player player, int curioSlot, int inventorySlot, ItemStack stack) {
        if (inventorySlot >= 0) {
            player.getInventory().setItem(inventorySlot, stack);
        } else if (curioSlot >= 0) {
            CuriosCompat.setBody(player, curioSlot, stack);
        } else {
            player.setItemSlot(EquipmentSlot.CHEST, stack);
        }
    }
}
