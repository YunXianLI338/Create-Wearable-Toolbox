package com.minecart.wearabletoolbox;

import com.minecart.wearabletoolbox.common.DepositAllPayload;
import com.minecart.wearabletoolbox.common.OpenToolboxPayload;
import com.minecart.wearabletoolbox.common.SyncToolboxPayload;
import com.minecart.wearabletoolbox.common.ToolboxPosePayload;
import com.minecart.wearabletoolbox.common.ToolboxPoseStatePayload;
import com.minecart.wearabletoolbox.common.WearableToolboxBindPayload;
import com.minecart.wearabletoolbox.common.WearableToolboxMenu;
import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

/**
 * 可穿戴的工具箱（Wearable Toolbox）主类。
 *
 * <p>模组 ID 必须与 {@code gradle.properties} 的 {@code mod_id} 以及
 * {@code META-INF/neoforge.mods.toml} 的 {@code modId} 保持一致。</p>
 */
@Mod(WearableToolbox.MODID)
public class WearableToolbox {
    public static final String MODID = "wearable_toolbox";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MODID);

    /**
     * 可穿戴工具箱的菜单类型。
     *
     * <p>不能直接复用 Create 的 {@code AllMenuTypes.TOOLBOX}：Create 的客户端菜单构造器
     * 会按方块坐标去客户端世界找 {@code ToolboxBlockEntity}，而饰品栏里的工具箱没有方块实体。
     * 这个菜单类型使用我们自己的网络数据（槽位 + 物品栈）来创建客户端菜单。</p>
     */
    public static final DeferredHolder<MenuType<?>, MenuType<WearableToolboxMenu>> TOOLBOX_MENU =
            MENUS.register("toolbox", () -> IMenuTypeExtension.create(WearableToolboxMenu::new));

    public WearableToolbox(IEventBus modEventBus, ModContainer modContainer) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modContainer.registerConfig(ModConfig.Type.CLIENT, WearableToolboxConfig.SPEC);
        }

        MENUS.register(modEventBus);
        modEventBus.addListener(WearableToolbox::registerPayloads);

        LOGGER.info("[{}] 可穿戴的工具箱已加载", MODID);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                OpenToolboxPayload.TYPE,
                OpenToolboxPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> OpenToolboxPayload.handle(context.player()))
        );
        registrar.playToServer(
                WearableToolboxBindPayload.TYPE,
                WearableToolboxBindPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> WearableToolboxBindPayload.handle(context.player(), payload))
        );
        registrar.playToServer(
                DepositAllPayload.TYPE,
                DepositAllPayload.STREAM_CODEC,
                DepositAllPayload::handle
        );
        registrar.playToClient(
                SyncToolboxPayload.TYPE,
                SyncToolboxPayload.STREAM_CODEC,
                SyncToolboxPayload::handle
        );
        registrar.playToServer(
                ToolboxPosePayload.TYPE,
                ToolboxPosePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(
                        () -> ToolboxPosePayload.handle(context.player(), payload))
        );
        registrar.playToClient(
                ToolboxPoseStatePayload.TYPE,
                ToolboxPoseStatePayload.STREAM_CODEC,
                ToolboxPoseStatePayload::handle
        );
    }
}
