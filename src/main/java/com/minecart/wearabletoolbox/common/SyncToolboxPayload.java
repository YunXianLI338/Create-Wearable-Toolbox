package com.minecart.wearabletoolbox.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * 服务端把工具箱物品同步给客户端。
 *
 * @param stack         最新工具箱物品
 * @param curioSlot     -1 为胸甲栏，>=0 为 Curios body 槽位
 * @param inventorySlot >=0 为物品栏槽位，-1 为已装备来源
 */
public record SyncToolboxPayload(ItemStack stack, int curioSlot, int inventorySlot) {

    public SyncToolboxPayload(FriendlyByteBuf buffer) {
        this(buffer.readItem(), buffer.readVarInt(), buffer.readVarInt());
    }

    public static void write(SyncToolboxPayload message, FriendlyByteBuf buffer) {
        buffer.writeItem(message.stack());
        buffer.writeVarInt(message.curioSlot());
        buffer.writeVarInt(message.inventorySlot());
    }
}
