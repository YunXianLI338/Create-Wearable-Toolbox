package com.minecart.wearabletoolbox.common;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

/**
 * 服务端把身上的工具箱物品同步给客户端。
 *
 * <p>自动补充会直接修改工具箱物品上的库存 NBT，而玩家打开径向菜单时读的是客户端
 * 本地物品栈。仅依赖原版装备同步在部分情况下会拿到旧数据，因此每次写回装备槽后
 * 主动发一份最新物品栈。</p>
 */
public record SyncToolboxPayload(ItemStack stack, int curioSlot) {

    public SyncToolboxPayload(FriendlyByteBuf buffer) {
        this(buffer.readItem(), buffer.readVarInt());
    }

    public static void write(SyncToolboxPayload message, FriendlyByteBuf buffer) {
        buffer.writeItem(message.stack());
        buffer.writeVarInt(message.curioSlot());
    }
}
