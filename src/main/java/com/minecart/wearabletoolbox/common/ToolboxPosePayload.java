package com.minecart.wearabletoolbox.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * 客户端在打开/关闭工具箱界面时发送，让服务端广播给其他玩家。
 */
public record ToolboxPosePayload(boolean posing) {

    public ToolboxPosePayload(FriendlyByteBuf buffer) {
        this(buffer.readBoolean());
    }

    public static void write(ToolboxPosePayload message, FriendlyByteBuf buffer) {
        buffer.writeBoolean(message.posing());
    }

    public static void handle(ServerPlayer player, ToolboxPosePayload payload) {
        // 只有真正装备着工具箱的玩家才能让其他人看到姿势。
        boolean posing = payload.posing() && EquippedToolbox.find(player) != null;
        ToolboxPoseState.setServer(player.getUUID(), posing);
        WearableToolboxNetwork.sendToTrackingAndSelf(player,
                new ToolboxPoseStatePayload(player.getUUID(), posing));
    }
}
