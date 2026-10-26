package com.minecart.wearabletoolbox.client;

import com.minecart.wearabletoolbox.WearableToolbox;
import com.minecart.wearabletoolbox.common.DepositAllPayload;
import com.minecart.wearabletoolbox.common.EquippedToolbox;
import com.minecart.wearabletoolbox.common.WearableToolboxBindings;
import com.minecart.wearabletoolbox.common.WearableToolboxMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.foundation.gui.AllGuiTextures;
import com.simibubi.create.foundation.gui.AllIcons;
import com.simibubi.create.foundation.gui.widget.IconButton;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.network.PacketDistributor;
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
     * 当前是否打开了可穿戴工具箱的两种界面之一。
     */
    public static boolean isToolboxScreenOpen() {
        Minecraft minecraft = Minecraft.getInstance();
        return minecraft.screen instanceof WearableToolboxScreen
                || minecraft.screen instanceof WearableToolboxRadialScreen;
    }

    @SubscribeEvent
    public static void addLayers(EntityRenderersEvent.AddLayers event) {
        for (PlayerSkin.Model skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new ToolboxArmorLayer(renderer));
            }
        }
    }

    @SubscribeEvent
    public static void registerGuiLayers(RegisterGuiLayersEvent event) {
        event.registerAbove(
                VanillaGuiLayers.HOTBAR,
                ResourceLocation.fromNamespaceAndPath(WearableToolbox.MODID, "toolbox_bindings"),
                WearableToolboxClient::renderToolboxBindings
        );
    }

    /**
     * 给通过可穿戴工具箱绑定的快捷栏槽位画 Create 同款黄铜色边框。
     * 放置版由 Create 自己根据方块距离绘制；可穿戴版没有坐标，绑定即视为可用。
     */
    private static void renderToolboxBindings(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.options.hideGui || minecraft.gameMode == null
                || minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR) {
            return;
        }
        if (EquippedToolbox.find(player) == null) {
            return;
        }

        CompoundTag root = player.getPersistentData().getCompound(WearableToolboxBindings.DATA_KEY);
        if (root.isEmpty()) {
            return;
        }

        int x = graphics.guiWidth() / 2 - 90;
        int y = graphics.guiHeight() - 23;
        PoseStack poseStack = graphics.pose();
        RenderSystem.enableDepthTest();
        poseStack.pushPose();
        for (int slot = 0; slot < Inventory.getSelectionSize(); slot++) {
            if (!root.contains(String.valueOf(slot))) {
                continue;
            }
            boolean selected = player.getInventory().selected == slot;
            int offset = selected ? 1 : 0;
            AllGuiTextures texture = selected
                    ? AllGuiTextures.TOOLBELT_SELECTED_ON
                    : AllGuiTextures.TOOLBELT_HOTBAR_ON;
            texture.render(graphics, x + 20 * slot - offset, y + offset);
        }
        poseStack.popPose();
    }

    /**
     * Create 的 ToolboxScreen 自带一个“全部存入附近工具箱”的按钮，它按方块坐标发包。
     * 可穿戴工具箱没有方块坐标，这里重新初始化界面时替换为向自身工具箱存入。
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

            IconButton deposit = new IconButton(
                    leftPos + 30 + 81,
                    topPos + 69,
                    AllIcons.I_TOOLBOX
            );
            deposit.setToolTip(Component.translatable("tooltip.wearable_toolbox.deposit_all"));
            deposit.withCallback(() -> PacketDistributor.sendToServer(new DepositAllPayload()));
            addRenderableWidget(deposit);

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
