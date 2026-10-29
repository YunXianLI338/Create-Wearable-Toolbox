package com.minecart.wearabletoolbox.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

/**
 * 服务端把玩家的快捷栏绑定数据同步给客户端，用于绘制热键栏边框。
 *
 * <p>1.21.1 复用了 Catnip 的 {@code ISyncPersistentData.PersistentDataPacket}，
 * 1.20.1 里换成自己的消息，只发本模组需要的那一小段 NBT。</p>
 */
public record SyncBindingsPayload(CompoundTag data) {

    public SyncBindingsPayload(FriendlyByteBuf buffer) {
        this(readData(buffer));
    }

    public static void write(SyncBindingsPayload message, FriendlyByteBuf buffer) {
        buffer.writeNbt(message.data());
    }

    private static CompoundTag readData(FriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        return tag == null ? new CompoundTag() : tag;
    }
}
