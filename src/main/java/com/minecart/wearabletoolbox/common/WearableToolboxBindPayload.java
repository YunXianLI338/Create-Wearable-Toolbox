package com.minecart.wearabletoolbox.common;

import com.minecart.wearabletoolbox.WearableToolbox;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * 径向菜单关闭时发送：把某个隔间绑定到当前快捷栏，或解除绑定。
 *
 * @param compartment   0-7 为隔间，-1 表示解除当前快捷栏的绑定
 * @param hotbarSlot    0-8
 * @param curioSlot     Curios body 槽位；-1 表示非 Curios
 * @param inventorySlot 物品栏槽位；-1 表示装备来源
 */
public record WearableToolboxBindPayload(int compartment, int hotbarSlot, int curioSlot, int inventorySlot)
        implements CustomPacketPayload {
    public static final Type<WearableToolboxBindPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WearableToolbox.MODID, "bind_toolbox"));

    public static final StreamCodec<ByteBuf, WearableToolboxBindPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_INT, WearableToolboxBindPayload::compartment,
                    ByteBufCodecs.VAR_INT, WearableToolboxBindPayload::hotbarSlot,
                    ByteBufCodecs.VAR_INT, WearableToolboxBindPayload::curioSlot,
                    ByteBufCodecs.VAR_INT, WearableToolboxBindPayload::inventorySlot,
                    WearableToolboxBindPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(Player player, WearableToolboxBindPayload payload) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (payload.compartment() < 0) {
            WearableToolboxBindings.unbind(serverPlayer, payload.hotbarSlot());
        } else {
            WearableToolboxBindings.bind(
                    serverPlayer,
                    payload.hotbarSlot(),
                    payload.compartment(),
                    payload.curioSlot(),
                    payload.inventorySlot()
            );
        }
    }
}
