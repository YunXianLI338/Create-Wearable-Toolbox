package com.minecart.wearabletoolbox.common;

import com.minecart.wearabletoolbox.WearableToolbox;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 客户端在打开/关闭工具箱界面时发送，让服务端广播给其他玩家。
 */
public record ToolboxPosePayload(boolean posing) implements CustomPacketPayload {
    public static final Type<ToolboxPosePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WearableToolbox.MODID, "toolbox_pose"));

    public static final StreamCodec<ByteBuf, ToolboxPosePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BOOL, ToolboxPosePayload::posing,
                    ToolboxPosePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(Player player, ToolboxPosePayload payload) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        // 只有真正装备着工具箱的玩家才能让其他人看到姿势。
        boolean posing = payload.posing() && EquippedToolbox.find(serverPlayer) != null;
        ToolboxPoseState.setServer(serverPlayer.getUUID(), posing);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                serverPlayer,
                new ToolboxPoseStatePayload(serverPlayer.getUUID(), posing)
        );
    }
}
