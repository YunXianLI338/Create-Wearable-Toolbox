package com.minecart.wearabletoolbox;

import com.minecart.wearabletoolbox.common.WearableToolboxBindings;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
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

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompoundTag root = player.getPersistentData().getCompound(WearableToolboxBindings.DATA_KEY);
            if (!root.isEmpty()) {
                // 客户端重新登录时本地没有绑定数据，需要主动同步一次以恢复快捷栏边框。
                ToolboxHandler.syncData(player);
            }
        }
    }
}
