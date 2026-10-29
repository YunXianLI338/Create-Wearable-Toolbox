package com.minecart.wearabletoolbox.common;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

/**
 * 服务端广播：某个玩家是否正在摆“打开工具箱”的姿势。
 */
public record ToolboxPoseStatePayload(UUID playerId, boolean posing) {

    public ToolboxPoseStatePayload(FriendlyByteBuf buffer) {
        this(buffer.readUUID(), buffer.readBoolean());
    }

    public static void write(ToolboxPoseStatePayload message, FriendlyByteBuf buffer) {
        buffer.writeUUID(message.playerId());
        buffer.writeBoolean(message.posing());
    }

    public static void handle(ToolboxPoseStatePayload payload) {
        ToolboxPoseState.setClient(payload.playerId(), payload.posing());
    }
}
