package com.minecart.wearabletoolbox.client;

import com.minecart.wearabletoolbox.WearableToolbox;
import com.minecart.wearabletoolbox.common.ToolboxItems;
import com.simibubi.create.AllKeys;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * 工具箱物品的操作提示。
 *
 * <p>按键名全部从实际 {@code KeyMapping} 读取：V 是模组自己的打开键，
 * Alt 是 Create 的“工具箱”键，Shift 是原版的潜行键。玩家改键后 tooltip 会跟着变。</p>
 */
@EventBusSubscriber(modid = WearableToolbox.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class WearableToolboxClientTooltips {
    private WearableToolboxClientTooltips() {
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (!ToolboxItems.isToolbox(event.getItemStack())) {
            return;
        }

        Component openKey = WearableToolboxClient.OPEN_TOOLBOX.getTranslatedKeyMessage();
        Component altKey = AllKeys.TOOLBELT.getKeybind().getTranslatedKeyMessage();
        Component shiftKey = Minecraft.getInstance().options.keyShift.getTranslatedKeyMessage();

        event.getToolTip().add(Component.translatable("tooltip.wearable_toolbox.equip")
                .withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable("tooltip.wearable_toolbox.open", openKey)
                .withStyle(ChatFormatting.DARK_GRAY));
        event.getToolTip().add(Component.translatable("tooltip.wearable_toolbox.alt", altKey)
                .withStyle(ChatFormatting.DARK_GRAY));
        event.getToolTip().add(Component.translatable("tooltip.wearable_toolbox.quick_equip", shiftKey)
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
