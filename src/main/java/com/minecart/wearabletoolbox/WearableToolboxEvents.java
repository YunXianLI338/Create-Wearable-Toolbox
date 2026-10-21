package com.minecart.wearabletoolbox;

import com.minecart.wearabletoolbox.common.ToolboxItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * 通用游戏事件：给机械动力的工具箱物品加装备/操作提示。
 */
@EventBusSubscriber(modid = WearableToolbox.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class WearableToolboxEvents {
    private WearableToolboxEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (ToolboxItems.isToolbox(event.getItemStack())) {
            event.getToolTip().add(Component.translatable("tooltip.wearable_toolbox.equip")
                    .withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.translatable("tooltip.wearable_toolbox.open")
                    .withStyle(ChatFormatting.DARK_GRAY));
            event.getToolTip().add(Component.translatable("tooltip.wearable_toolbox.alt")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
