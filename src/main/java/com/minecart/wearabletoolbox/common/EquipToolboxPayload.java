package com.minecart.wearabletoolbox.common;

import com.minecart.wearabletoolbox.WearableToolbox;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端请求：把背包里的一个工具箱快捷穿戴到胸甲栏。
 */
public record EquipToolboxPayload(int inventorySlot) implements CustomPacketPayload {
    public static final Type<EquipToolboxPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WearableToolbox.MODID, "equip_toolbox"));
    public static final StreamCodec<RegistryFriendlyByteBuf, EquipToolboxPayload> STREAM_CODEC =
            StreamCodec.composite(
                    net.minecraft.network.codec.ByteBufCodecs.VAR_INT,
                    EquipToolboxPayload::inventorySlot,
                    EquipToolboxPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(EquipToolboxPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        int slot = payload.inventorySlot();
        if (slot < 0 || slot >= 36) {
            return;
        }

        ItemStack stack = player.getInventory().getItem(slot);
        if (!ToolboxItems.isToolbox(stack)) {
            return;
        }

        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        // 与原版盔甲一致：胸甲栏被占用时不强行替换，避免把正在穿的胸甲顶掉。
        if (!chest.isEmpty()) {
            return;
        }
        player.getInventory().setItem(slot, ItemStack.EMPTY);
        player.setItemSlot(EquipmentSlot.CHEST, stack);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
    }
}
