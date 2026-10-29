package com.minecart.wearabletoolbox.common;

import com.minecart.wearabletoolbox.WearableToolbox;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * 服务端广播某个玩家是否正在摆“使用工具箱”的姿势。
 */
public record ToolboxPoseStatePayload(UUID playerId, boolean posing) implements CustomPacketPayload {
    public static final Type<ToolboxPoseStatePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WearableToolbox.MODID, "toolbox_pose_state"));

    public static final StreamCodec<ByteBuf, ToolboxPoseStatePayload> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, ToolboxPoseStatePayload::playerId,
                    ByteBufCodecs.BOOL, ToolboxPoseStatePayload::posing,
                    ToolboxPoseStatePayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToolboxPoseStatePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ToolboxPoseState.setClient(payload.playerId(), payload.posing()));
    }
}
