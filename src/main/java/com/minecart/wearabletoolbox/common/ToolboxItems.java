package com.minecart.wearabletoolbox.common;

import com.simibubi.create.content.equipment.toolbox.ToolboxBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;

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
}
