package com.minecart.wearabletoolbox.common;

import com.minecart.wearabletoolbox.WearableToolbox;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * 服务端把身上的工具箱物品同步给客户端。
 *
 * <p>自动补充会直接修改胸甲栏物品的 {@code create:toolbox_inventory} 组件，而玩家打开
 * 径向菜单时读的是客户端本地物品栈。仅依赖原版装备同步在部分情况下会拿到旧组件，
 * 因此每次写回装备槽后主动发一份最新物品栈。</p>
 */
public record SyncToolboxPayload(ItemStack stack, int curioSlot) implements CustomPacketPayload {
    public static final Type<SyncToolboxPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(WearableToolbox.MODID, "sync_toolbox"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncToolboxPayload> STREAM_CODEC =
            StreamCodec.composite(
                    ItemStack.STREAM_CODEC, SyncToolboxPayload::stack,
                    ByteBufCodecs.VAR_INT, SyncToolboxPayload::curioSlot,
                    SyncToolboxPayload::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncToolboxPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Player player = context.player();
            if (player != null) {
                EquippedToolbox.set(player, payload.curioSlot, payload.stack);
            }
        });
    }
}
