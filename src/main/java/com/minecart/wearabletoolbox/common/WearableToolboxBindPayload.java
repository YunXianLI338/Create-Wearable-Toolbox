package com.minecart.wearabletoolbox.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

/**
 * 径向菜单关闭时发送：把某个隔间绑定到当前快捷栏，或解除绑定。
 *
 * @param compartment 0-7 为隔间，-1 表示解除当前快捷栏的绑定
 * @param hotbarSlot  0-8
 */
public record WearableToolboxBindPayload(int compartment, int hotbarSlot) {

    public WearableToolboxBindPayload(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void write(WearableToolboxBindPayload message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.compartment());
        buffer.writeVarInt(message.hotbarSlot());
    }

    public static void handle(ServerPlayer player, WearableToolboxBindPayload payload) {
        if (payload.compartment() < 0) {
            WearableToolboxBindings.unbind(player, payload.hotbarSlot());
        } else {
            WearableToolboxBindings.bind(player, payload.hotbarSlot(), payload.compartment());
        }
    }
}
