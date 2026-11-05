package com.minecart.wearabletoolbox.common;

import com.minecart.wearabletoolbox.WearableToolbox;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * 客户端按“打开工具箱”键时发给服务端的请求。
 *
 * @param curioSlot     Curios body 槽位；-1 表示非 Curios
 * @param inventorySlot 物品栏槽位；-1 表示装备来源
 */
public record OpenToolboxPayload(int curioSlot, int inventorySlot) implements CustomPacketPayload {
    public static final Type<OpenToolboxPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WearableToolbox.MODID, "open_toolbox"));

    public static final StreamCodec<ByteBuf, OpenToolboxPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, OpenToolboxPayload::curioSlot,
                    ByteBufCodecs.VAR_INT, OpenToolboxPayload::inventorySlot,
                    OpenToolboxPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(Player player, OpenToolboxPayload payload) {
        WearableToolboxMenu.openFor(
                player,
                EquippedToolbox.find(player, payload.curioSlot(), payload.inventorySlot())
        );
    }
}
