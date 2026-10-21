package com.minecart.wearabletoolbox;

import com.minecart.wearabletoolbox.common.WearableToolboxBindings;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 服务端每 tick 维护可穿戴工具箱的快捷栏绑定与自动补充。
 */
@EventBusSubscriber(modid = WearableToolbox.MODID, bus = EventBusSubscriber.Bus.GAME)
public final class WearableToolboxServerEvents {
    private WearableToolboxServerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            WearableToolboxBindings.tick(player);
        }
    }
}
