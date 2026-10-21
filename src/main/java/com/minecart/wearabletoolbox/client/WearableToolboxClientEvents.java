package com.minecart.wearabletoolbox.client;

import com.minecart.wearabletoolbox.WearableToolbox;
import com.minecart.wearabletoolbox.common.OpenToolboxPayload;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * 客户端按键处理：按“打开工具箱”键时请求服务端打开胸甲栏中的工具箱。
 */
@EventBusSubscriber(modid = WearableToolbox.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class WearableToolboxClientEvents {
    private WearableToolboxClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null) {
            return;
        }

        while (WearableToolboxClient.OPEN_TOOLBOX.consumeClick()) {
            PacketDistributor.sendToServer(new OpenToolboxPayload());
        }
    }
}
