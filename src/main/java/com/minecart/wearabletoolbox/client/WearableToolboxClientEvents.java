package com.minecart.wearabletoolbox.client;

import com.minecart.wearabletoolbox.WearableToolbox;
import com.minecart.wearabletoolbox.WearableToolboxConfig;
import com.minecart.wearabletoolbox.common.EquippedToolbox;
import com.minecart.wearabletoolbox.common.OpenToolboxPayload;
import com.minecart.wearabletoolbox.common.ToolboxItems;
import com.minecart.wearabletoolbox.common.ToolboxPosePayload;
import com.minecart.wearabletoolbox.common.ToolboxPoseState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllItems;
import com.simibubi.create.AllKeys;
import com.simibubi.create.content.equipment.toolbox.ToolboxBlockEntity;
import com.simibubi.create.content.equipment.toolbox.ToolboxHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.ArmorStandRenderer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.Mth;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 客户端按键处理：
 * <ul>
 *   <li>V：打开身上工具箱的 8 隔间界面；</li>
 *   <li>Create 的“工具箱”键（默认左 Alt）：打开可穿戴工具箱的径向菜单。</li>
 * </ul>
 */
@EventBusSubscriber(modid = WearableToolbox.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public final class WearableToolboxClientEvents {
    private static boolean lastPosing;
    private static boolean wasToolboxMenuOpen;
    private static boolean suppressCreateMenu;

    private WearableToolboxClientEvents() {
    }

    public static boolean shouldSuppressCreateMenu() {
        return suppressCreateMenu;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        boolean screenOpen = WearableToolboxClient.isToolboxScreenOpen();
        boolean posing = screenOpen && EquippedToolbox.find(minecraft.player) != null;
        boolean menuOpen = WearableToolboxClient.isToolboxMenuScreenOpen();
        if (menuOpen != wasToolboxMenuOpen) {
            if (menuOpen) {
                WearableToolboxClient.playToolboxOpenSound(minecraft.player);
            } else {
                WearableToolboxClient.playToolboxCloseSound(minecraft.player);
            }
        }
        wasToolboxMenuOpen = menuOpen;
        if (posing != lastPosing) {
            PacketDistributor.sendToServer(new ToolboxPosePayload(posing));
            lastPosing = posing;
        }

        if (minecraft.screen != null) {
            return;
        }

        while (WearableToolboxClient.OPEN_TOOLBOX.consumeClick()) {
            List<EquippedToolbox> choices = candidates(minecraft.player);
            if (choices.size() == 1) {
                EquippedToolbox source = choices.get(0);
                PacketDistributor.sendToServer(
                        new OpenToolboxPayload(source.curioSlot(), source.inventorySlot())
                );
            } else if (!choices.isEmpty()) {
                WearableToolboxRadialScreen.openMenuChoice(choices, minecraft.player);
            }
        }
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        lastPosing = false;
        wasToolboxMenuOpen = false;
        ToolboxPoseState.clearClient();
    }

    /**
     * 按配置决定本次 Alt 是打开穿戴版、放置版，还是先打开“选择工具箱”界面。
     */
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void onKeyInput(InputEvent.Key event) {
        suppressCreateMenu = false;
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

        WearableToolboxConfig.Priority priority = WearableToolboxConfig.priority();
        List<ToolboxBlockEntity> nearby = ToolboxHandler.getNearest(player.level(), player, 8);
        List<EquippedToolbox> equippedBoxes = candidates(player);

        switch (priority) {
            case WEARABLE -> {
                if (!equippedBoxes.isEmpty()) {
                    suppressCreateMenu = true;
                    openEquipped(equippedBoxes, List.of(), player);
                }
            }
            case PLACED -> {
                // 周围有放置版时交还给 Create；只有没有放置版时才打开穿戴版。
                if (!nearby.isEmpty() || equippedBoxes.isEmpty()) {
                    return;
                }
                suppressCreateMenu = true;
                openEquipped(equippedBoxes, List.of(), player);
            }
            case EQUAL -> {
                if (equippedBoxes.isEmpty()) {
                    // 没有穿戴版，交给 Create 处理放置版。
                    return;
                }
                suppressCreateMenu = true;
                if (nearby.isEmpty()) {
                    openEquipped(equippedBoxes, List.of(), player);
                } else {
                    openEquipped(equippedBoxes, nearby, player);
                }
            }
        }
    }

    private static List<EquippedToolbox> candidates(LocalPlayer player) {
        return WearableToolboxConfig.inventoryToolboxes()
                ? EquippedToolbox.findAllIncludingInventory(player)
                : EquippedToolbox.findAll(player);
    }

    private static void openEquipped(List<EquippedToolbox> equippedBoxes,
                                     List<ToolboxBlockEntity> nearby,
                                     LocalPlayer player) {
        if (equippedBoxes.size() == 1 && nearby.isEmpty()) {
            WearableToolboxRadialScreen.openWearable(equippedBoxes.get(0), player);
        } else {
            WearableToolboxRadialScreen.openSelection(equippedBoxes, nearby, player);
        }
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

        // RenderLivingEvent.Post 是在 LivingEntityRenderer 的 popPose() 之后触发的，
        // 此时 PoseStack 里只剩下世界位移，盔甲架自身的偏航角已经被弹掉了。
        // 不自己补上这一转，工具箱就会永远朝同一个世界方向，不跟着盔甲架转。
        float yaw = Mth.rotLerp(event.getPartialTick(), stand.yBodyRotO, stand.yBodyRot);
        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F - yaw));
        ToolboxArmorLayer.renderModel(
                poseStack,
                event.getMultiBufferSource(),
                event.getPackedLight(),
                renderer.getModel().body,
                false,
                true,
                ToolboxItems.color(chest)
        );
        poseStack.popPose();
    }
}
