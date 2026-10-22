package com.minecart.wearabletoolbox.common;

import com.simibubi.create.content.equipment.toolbox.ToolboxBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 判断一个物品栈是不是机械动力的工具箱方块物品。
 */
public final class ToolboxItems {
    private ToolboxItems() {
    }

    public static boolean isToolbox(ItemStack stack) {
        return stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ToolboxBlock;
    }

    /**
     * 读取工具箱方块物品的染色颜色；不是工具箱时返回 {@code null}。
     */
    @Nullable
    public static DyeColor color(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem blockItem
                && blockItem.getBlock() instanceof ToolboxBlock toolbox) {
            return toolbox.getColor();
        }
        return null;
    }
}
