package com.minecart.wearabletoolbox.common;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * 玩家身上或物品栏中的工具箱。
 *
 * <p>工具箱可以放在原版胸甲栏、Curios 的“胸饰（body）”饰品栏，或普通物品栏。
 * 查询时优先胸甲栏；Curios 未安装时只检查胸甲栏。</p>
 *
 * @param stack         工具箱物品
 * @param curioSlot     Curios body 槽位下标；-1 表示非 Curios 来源
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

    /**
     * 工具箱在当前槽位上是否应渲染模型。物品栏中的工具箱不渲染在玩家身上。
     */
    public boolean isVisibleOn(Player player) {
        return !isInventory() && (!isCurio() || CuriosCompat.isBodySlotVisible(player, curioSlot));
    }

    /**
     * 玩家身上用于渲染的工具箱：优先按槽位顺序取第一个未隐藏的 Curios body 工具箱；
     * 没有可见饰品栏工具箱时再退回胸甲栏。
     */
    @Nullable
    public static EquippedToolbox findForRender(Player player) {
        if (ModList.get().isLoaded("curios")) {
            for (EquippedToolbox curio : CuriosCompat.findAllBodies(player)) {
                if (curio.isVisibleOn(player)) {
                    return curio;
                }
            }
        }

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        return ToolboxItems.isToolbox(chest) ? new EquippedToolbox(chest, -1) : null;
    }

    @Nullable
    public static EquippedToolbox find(Player player) {
        List<EquippedToolbox> all = findAll(player);
        return all.isEmpty() ? null : all.get(0);
    }

    /**
     * 按装备位置解析工具箱：-1 为原版胸甲栏，>=0 为 Curios body 槽位。
     */
    @Nullable
    public static EquippedToolbox find(Player player, int curioSlot) {
        return find(player, curioSlot, -1);
    }

    /**
     * 按来源解析：inventorySlot >= 0 时从物品栏取；否则按 curioSlot 取装备槽。
     */
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

    /**
     * 玩家身上所有工具箱：胸甲栏在前，其后是各 Curios body 槽位。
     */
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

    /**
     * 身上与物品栏中的所有工具箱。物品栏顺序为 0..35。
     */
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

    /**
     * 把修改后的工具箱写回它原来所在的装备槽或物品栏槽位。
     */
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
