package com.minecart.wearabletoolbox.common;

import com.minecart.wearabletoolbox.WearableToolbox;
import com.minecart.wearabletoolbox.client.ClientPacketHandlers;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 1.20.1 的网络层。
 *
 * <p>1.21.1 用的是 {@code CustomPacketPayload} + {@code PayloadRegistrar}；1.20.1 的 Forge
 * 只有 {@link SimpleChannel}，因此这里用同一套包装重新登记全部消息。
 * 消息体仍是普通的 record，编码逻辑写在各自的 {@code write}/{@code read} 里。</p>
 */
public final class WearableToolboxNetwork {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(WearableToolbox.MODID, "main"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    private static int nextId;

    private WearableToolboxNetwork() {
    }

    public static void register() {
        CHANNEL.messageBuilder(OpenToolboxPayload.class, nextId++)
                .encoder(OpenToolboxPayload::write)
                .decoder(OpenToolboxPayload::new)
                .consumerMainThread((message, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) {
                        WearableToolboxMenu.openFor(player);
                    }
                    context.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(WearableToolboxBindPayload.class, nextId++)
                .encoder(WearableToolboxBindPayload::write)
                .decoder(WearableToolboxBindPayload::new)
                .consumerMainThread((message, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) {
                        WearableToolboxBindPayload.handle(player, message);
                    }
                    context.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(DepositAllPayload.class, nextId++)
                .encoder(DepositAllPayload::write)
                .decoder(DepositAllPayload::new)
                .consumerMainThread((message, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) {
                        WearableToolboxMenu.depositAll(player);
                    }
                    context.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(ToolboxPosePayload.class, nextId++)
                .encoder(ToolboxPosePayload::write)
                .decoder(ToolboxPosePayload::new)
                .consumerMainThread((message, context) -> {
                    ServerPlayer player = context.get().getSender();
                    if (player != null) {
                        ToolboxPosePayload.handle(player, message);
                    }
                    context.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(SyncToolboxPayload.class, nextId++)
                .encoder(SyncToolboxPayload::write)
                .decoder(SyncToolboxPayload::new)
                .consumerMainThread((message, context) -> {
                    if (context.get().getDirection().getReceptionSide().isClient()) {
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                                () -> () -> ClientPacketHandlers.syncToolbox(message));
                    }
                    context.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(ToolboxPoseStatePayload.class, nextId++)
                .encoder(ToolboxPoseStatePayload::write)
                .decoder(ToolboxPoseStatePayload::new)
                .consumerMainThread((message, context) -> {
                    ToolboxPoseStatePayload.handle(message);
                    context.get().setPacketHandled(true);
                })
                .add();

        CHANNEL.messageBuilder(SyncBindingsPayload.class, nextId++)
                .encoder(SyncBindingsPayload::write)
                .decoder(SyncBindingsPayload::new)
                .consumerMainThread((message, context) -> {
                    if (context.get().getDirection().getReceptionSide().isClient()) {
                        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                                () -> () -> ClientPacketHandlers.syncBindings(message));
                    }
                    context.get().setPacketHandled(true);
                })
                .add();
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }

    public static void sendToTrackingAndSelf(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), message);
    }
}
