package com.minecart.wearabletoolbox;

import com.minecart.wearabletoolbox.common.ToolboxItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 通用游戏事件：给机械动力的工具箱物品加装备/操作提示，并允许把工具箱穿到盔甲架上。
 */
@Mod.EventBusSubscriber(modid = WearableToolbox.MODID)
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
            event.getToolTip().add(Component.translatable("tooltip.wearable_toolbox.quick_equip")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /**
     * 手持工具箱右键盔甲架：放到胸甲槽，原有胸甲退回玩家背包。
     *
     * <p>盔甲架重写的是 {@code interactAt}，对应 {@link PlayerInteractEvent.EntityInteractSpecific}；
     * 只监听 {@code EntityInteract} 不会触发。</p>
     */
    @SubscribeEvent
    public static void onArmorStandInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        if (!(event.getTarget() instanceof ArmorStand stand)) {
            return;
        }

        Player player = event.getEntity();
        ItemStack held = player.getItemInHand(event.getHand());
        if (!ToolboxItems.isToolbox(held)) {
            return;
        }

        if (!player.level().isClientSide) {
            ItemStack previous = stand.getItemBySlot(EquipmentSlot.CHEST);
            if (!previous.isEmpty()) {
                player.getInventory().placeItemBackInInventory(previous.copy());
            }

            stand.setItemSlot(EquipmentSlot.CHEST, held.copy());
            if (!player.isCreative()) {
                held.shrink(1);
            }
        }

        event.setCancellationResult(InteractionResult.sidedSuccess(player.level().isClientSide));
        event.setCanceled(true);
    }
}
