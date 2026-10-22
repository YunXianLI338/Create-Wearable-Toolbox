package com.minecart.wearabletoolbox.common;

import com.minecart.wearabletoolbox.WearableToolbox;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 客户端请求：把玩家背包里能放进当前工具箱的物品全部存入。
 */
public record DepositAllPayload() implements CustomPacketPayload {
    public static final Type<DepositAllPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WearableToolbox.MODID, "deposit_all"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DepositAllPayload> STREAM_CODEC =
            StreamCodec.unit(new DepositAllPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DepositAllPayload payload, IPayloadContext context) {
        if (context.player() instanceof ServerPlayer player) {
            context.enqueueWork(() -> WearableToolboxMenu.depositAll(player));
        }
    }
}
