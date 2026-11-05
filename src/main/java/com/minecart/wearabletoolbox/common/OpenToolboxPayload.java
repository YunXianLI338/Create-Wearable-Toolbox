package com.minecart.wearabletoolbox.common;

import net.minecraft.network.FriendlyByteBuf;

/**
 * 客户端请求：打开指定来源的工具箱界面。
 *
 * @param curioSlot     -1 为胸甲栏，>=0 为 Curios body 槽位
 * @param inventorySlot >=0 为物品栏槽位，-1 为已装备来源
 */
public record OpenToolboxPayload(int curioSlot, int inventorySlot) {

    public OpenToolboxPayload(FriendlyByteBuf buffer) {
        this(buffer.readVarInt(), buffer.readVarInt());
    }

    public static void write(OpenToolboxPayload message, FriendlyByteBuf buffer) {
        buffer.writeVarInt(message.curioSlot());
        buffer.writeVarInt(message.inventorySlot());
    }
}
