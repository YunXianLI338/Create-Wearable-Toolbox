package com.minecart.wearabletoolbox;

import com.minecart.wearabletoolbox.common.WearableToolboxMenu;
import com.minecart.wearabletoolbox.common.WearableToolboxNetwork;
import com.mojang.logging.LogUtils;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/**
 * 可穿戴的工具箱（Wearable Toolbox）主类。
 *
 * <p>模组 ID 必须与 {@code gradle.properties} 的 {@code mod_id} 以及
 * {@code META-INF/mods.toml} 的 {@code modId} 保持一致。</p>
 */
@Mod(WearableToolbox.MODID)
public class WearableToolbox {
    public static final String MODID = "wearable_toolbox";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, MODID);

    /**
     * 可穿戴工具箱的菜单类型。
     *
     * <p>不能直接复用 Create 的 {@code AllMenuTypes.TOOLBOX}：Create 的客户端菜单构造器
     * 会按方块坐标去客户端世界找 {@code ToolboxBlockEntity}，而饰品栏里的工具箱没有方块实体。
     * 这个菜单类型使用我们自己的网络数据（槽位 + 物品栈）来创建客户端菜单。</p>
     */
    public static final RegistryObject<MenuType<WearableToolboxMenu>> TOOLBOX_MENU =
            MENUS.register("toolbox", () -> IForgeMenuType.create(WearableToolboxMenu::new));

    public WearableToolbox() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, WearableToolboxConfig.SPEC);
        }

        MENUS.register(modEventBus);
        WearableToolboxNetwork.register();

        LOGGER.info("[{}] 可穿戴的工具箱已加载", MODID);
    }
}
