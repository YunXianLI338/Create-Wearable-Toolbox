package com.minecart.wearabletoolbox;

import com.minecart.wearabletoolbox.common.WearableToolboxMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * 通用游戏事件：给机械动力的工具箱物品加一行“可放入饰品栏”的提示。
 */
@EventBusSubscriber(modid = WearableToolbox.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class WearableToolboxEvents {
    private WearableToolboxEvents() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (WearableToolboxMenu.isToolbox(event.getItemStack())) {
            event.getToolTip().add(Component.translatable("tooltip.wearable_toolbox.equip")
                    .withStyle(ChatFormatting.GRAY));
            event.getToolTip().add(Component.translatable("tooltip.wearable_toolbox.open")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
