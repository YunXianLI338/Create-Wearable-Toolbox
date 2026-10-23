package com.minecart.wearabletoolbox.client;

import com.minecart.wearabletoolbox.WearableToolbox;
import com.minecart.wearabletoolbox.common.EquipToolboxPayload;
import com.minecart.wearabletoolbox.common.EquippedToolbox;
import com.minecart.wearabletoolbox.common.OpenToolboxPayload;
import com.minecart.wearabletoolbox.common.ToolboxItems;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllKeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 客户端按键处理：
 * <ul>
 *   <li>V：打开身上工具箱的 8 隔间界面；</li>
 *   <li>Create 的“工具箱”键（默认左 Alt）：打开可穿戴工具箱的径向菜单。</li>
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
            if (EquippedToolbox.find(minecraft.player) != null) {
                PacketDistributor.sendToServer(new OpenToolboxPayload());
            }
        }
    }

    /**
     * 玩家身上有工具箱时，优先于附近的放置版工具箱，直接打开可穿戴版径向菜单。
     */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
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
        // 手持已写入文件的蓝图时，Alt 是 Create 的蓝图工具菜单快捷键，优先让给手持物品。
        if (holdsSchematic(player)) {
            return;
        }

        EquippedToolbox equipped = EquippedToolbox.find(player);
        if (equipped == null) {
            return;
        }

        WearableToolboxRadialScreen.open(equipped.stack(), player);
    }

    /**
     * Create 的蓝图工具菜单只在主手持“带 schematic_file 的蓝图”时激活。
     */
    private static boolean holdsSchematic(LocalPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        return AllItems.SCHEMATIC.isIn(mainHand)
                && mainHand.has(AllDataComponents.SCHEMATIC_FILE);
    }

    /**
     * 物品栏里 Shift + 左键/右键工具箱：快捷穿到胸甲栏。
     *
     * <p>原版 {@code InventoryMenu#quickMoveStack} 只对 {@code getEquipmentSlotForItem}
     * 返回盔甲槽的物品自动穿戴；工具箱是 BlockItem，这个方法返回主手，所以需要在这里
     * 自己拦截 Shift 点击。只拦截玩家物品栏界面，工作台、箱子等工作台 GUI 仍走原版
     * 快速移动逻辑，避免把工具箱穿到身上而不是放进容器。</p>
     */
    @SubscribeEvent
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_LEFT
                && event.getButton() != GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !(event.getScreen() instanceof AbstractContainerScreen<?> screen)
                || !screen.hasShiftDown()) {
            return;
        }

        // 与原版盔甲一致：胸甲栏已有物品时不强行替换，交给原版 Shift 点击处理。
        if (!minecraft.player.getItemBySlot(EquipmentSlot.CHEST).isEmpty()) {
            return;
        }

        Slot slot = screen.getSlotUnderMouse();
        if (slot == null || slot.container != minecraft.player.getInventory()) {
            return;
        }

        int index = slot.getContainerSlot();
        if (index < 0 || index >= 36 || !ToolboxItems.isToolbox(slot.getItem())) {
            return;
        }

        PacketDistributor.sendToServer(new EquipToolboxPayload(index));
        event.setCanceled(true);
    }

    /**
     * 盔甲架胸甲槽里的工具箱也要显示展示模型。
     */
    @SubscribeEvent
    public static void onRenderLiving(RenderLivingEvent.Post event) {
        if (!(event.getEntity() instanceof ArmorStand stand)
                || !(event.getRenderer() instanceof ArmorStandRenderer renderer)) {
            return;
        }

        ItemStack chest = stand.getItemBySlot(EquipmentSlot.CHEST);
        if (!ToolboxItems.isToolbox(chest)) {
            return;
        }

        ToolboxArmorLayer.renderModel(
                event.getPoseStack(),
                event.getMultiBufferSource(),
                event.getPackedLight(),
                renderer.getModel().body,
                false,
                true,
                ToolboxItems.color(chest)
        );
    }
}
