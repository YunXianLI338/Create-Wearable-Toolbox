package com.minecart.wearabletoolbox.client;

import com.minecart.wearabletoolbox.WearableToolbox;
import com.minecart.wearabletoolbox.common.WearableToolboxMenu;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端注册：按键与菜单界面。
 */
@EventBusSubscriber(modid = WearableToolbox.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class WearableToolboxClient {
    public static final KeyMapping OPEN_TOOLBOX = new KeyMapping(
            "key.wearable_toolbox.open",
            GLFW.GLFW_KEY_V,
            "key.categories.wearable_toolbox"
    );

    private WearableToolboxClient() {
    }

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_TOOLBOX);
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(WearableToolbox.TOOLBOX_MENU.get(), WearableToolboxScreen::new);
    }

    /**
     * Create 的 ToolboxScreen 自带一个“全部存入附近工具箱”的按钮，它按方块坐标发包。
     * 可穿戴工具箱没有方块坐标，这里重新初始化界面时只保留确认按钮。
     */
    public static class WearableToolboxScreen extends com.simibubi.create.content.equipment.toolbox.ToolboxScreen {
        public WearableToolboxScreen(com.simibubi.create.content.equipment.toolbox.ToolboxMenu menu,
                                     Inventory inventory, Component title) {
            super(menu, inventory, title);
        }

        @Override
        protected void init() {
            super.init();
            clearWidgets();

            IconButton confirm = new IconButton(
                    leftPos + 30 + BG.getWidth() - 33,
                    topPos + BG.getHeight() - 24,
                    AllIcons.I_CONFIRM
            );
            confirm.withCallback(() -> {
                if (minecraft != null && minecraft.player != null) {
                    minecraft.player.closeContainer();
                }
            });
            addRenderableWidget(confirm);
        }
    }
}
