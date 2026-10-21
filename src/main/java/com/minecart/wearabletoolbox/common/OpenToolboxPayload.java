package com.minecart.wearabletoolbox.common;

import com.minecart.wearabletoolbox.WearableToolbox;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 客户端按“打开工具箱”键时发给服务端的空包。菜单必须在服务端创建。
 */
public record OpenToolboxPayload() implements CustomPacketPayload {
    public static final Type<OpenToolboxPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WearableToolbox.MODID, "open_toolbox"));

    public static final StreamCodec<ByteBuf, OpenToolboxPayload> STREAM_CODEC =
            StreamCodec.unit(new OpenToolboxPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(Player player) {
        WearableToolboxMenu.openFor(player);
    }
}
