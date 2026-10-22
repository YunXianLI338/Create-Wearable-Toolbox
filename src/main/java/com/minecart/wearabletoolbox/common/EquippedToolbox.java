package com.minecart.wearabletoolbox.common;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import javax.annotation.Nullable;

/**
 * 玩家身上已装备的工具箱。
 *
 * <p>工具箱可以放在原版胸甲栏，也可以放进 Curios 的“胸饰（body）”饰品栏。
 * 查询时优先胸甲栏；Curios 未安装时只检查胸甲栏。</p>
 *
 * @param stack   工具箱物品
 * @param curioSlot Curios body 槽位下标；-1 表示原版胸甲栏
 */
public record EquippedToolbox(ItemStack stack, int curioSlot) {
    public static final String CURIO_SLOT = "body";

    public boolean isCurio() {
        return curioSlot >= 0;
    }

    @Nullable
    public static EquippedToolbox find(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (ToolboxItems.isToolbox(chest)) {
            return new EquippedToolbox(chest, -1);
        }

        if (ModList.get().isLoaded("curios")) {
            return CuriosCompat.findBody(player);
        }
        return null;
    }

    /**
     * 把修改后的工具箱写回它原来所在的装备槽。
     */
    public static void set(Player player, int curioSlot, ItemStack stack) {
        if (curioSlot >= 0) {
            CuriosCompat.setBody(player, curioSlot, stack);
        } else {
            player.setItemSlot(EquipmentSlot.CHEST, stack);
        }
    }
}
