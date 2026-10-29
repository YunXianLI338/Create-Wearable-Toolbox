package com.minecart.wearabletoolbox;

import com.minecart.wearabletoolbox.common.ToolboxPoseState;
import com.minecart.wearabletoolbox.common.ToolboxPoseStatePayload;
import com.minecart.wearabletoolbox.common.WearableToolboxBindings;
import com.minecart.wearabletoolbox.common.WearableToolboxNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 服务端每 tick 维护可穿戴工具箱的快捷栏绑定与自动补充。
 */
@Mod.EventBusSubscriber(modid = WearableToolbox.MODID)
public final class WearableToolboxServerEvents {
    private WearableToolboxServerEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && event.player instanceof ServerPlayer player) {
            WearableToolboxBindings.tick(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompoundTag root = player.getPersistentData().getCompound(WearableToolboxBindings.DATA_KEY);
            if (!root.isEmpty()) {
                // 客户端重新登录时本地没有绑定数据，需要主动同步一次以恢复快捷栏边框。
                WearableToolboxBindings.syncData(player);
            }
        }
    }

    /**
     * 新进入追踪范围的玩家需要立即知道目标是否正在摆姿势，否则要等下一次开关界面。
     */
    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getTarget() instanceof ServerPlayer target
                && event.getEntity() instanceof ServerPlayer receiver) {
            WearableToolboxNetwork.sendToPlayer(receiver, new ToolboxPoseStatePayload(
                    target.getUUID(),
                    ToolboxPoseState.isServerPosing(target.getUUID())
            ));
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ToolboxPoseState.removeServer(player.getUUID());
        }
    }
}
