package com.minecart.wearabletoolbox;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
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

    public WearableToolbox(IEventBus modEventBus) {
        // mod bus（IEventBus）：模组生命周期事件与 DeferredRegister 的注册挂载点。
        // game bus（NeoForge.EVENT_BUS）：游戏运行期事件，需要时再注册监听器。
        LOGGER.info("[{}] 可穿戴的工具箱已加载", MODID);
    }
}
