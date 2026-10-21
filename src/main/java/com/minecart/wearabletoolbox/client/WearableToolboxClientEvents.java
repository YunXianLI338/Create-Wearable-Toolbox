package com.minecart.wearabletoolbox.client;

import com.minecart.wearabletoolbox.WearableToolbox;
import com.minecart.wearabletoolbox.common.OpenToolboxPayload;
import com.minecart.wearabletoolbox.common.ToolboxItems;
import com.simibubi.create.AllKeys;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端按键处理：
 * <ul>
 *   <li>V：打开胸甲栏工具箱的 8 隔间界面；</li>
 *   <li>Create 的“工具箱”键（默认左 Alt）：附近没有放置版工具箱时，打开可穿戴工具箱的径向菜单。</li>
 * </ul>
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

    /**
     * 低优先级：让 Create 先处理附近真实工具箱；它没有打开界面时，再由可穿戴工具箱接管。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onKeyInput(InputEvent.Key event) {
        if (event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.screen != null || minecraft.gameMode == null) {
            return;
        }
        if (minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return;
        }
        if (!AllKeys.TOOLBELT.doesModifierAndCodeMatch(event.getKey())) {
            return;
        }

        LocalPlayer player = minecraft.player;
        if (!ToolboxItems.isToolbox(player.getItemBySlot(EquipmentSlot.CHEST))) {
            return;
        }

        // 附近有真实工具箱时保持 Create 原行为，避免一次按键开出两个界面。
        if (!ToolboxHandler.getNearest(player.level(), player, 1).isEmpty()) {
            return;
        }

        WearableToolboxRadialScreen.open(player.getItemBySlot(EquipmentSlot.CHEST), player);
    }
}
