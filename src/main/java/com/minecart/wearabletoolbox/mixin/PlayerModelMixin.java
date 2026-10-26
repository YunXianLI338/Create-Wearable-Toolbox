package com.minecart.wearabletoolbox.mixin;

import com.minecart.wearabletoolbox.WearableToolboxConfig;
import com.minecart.wearabletoolbox.client.WearableToolboxClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 打开可穿戴工具箱的界面时，让本地玩家的双臂向前伸出，并保留轻微摆动。
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {
    @Inject(method = "setupAnim", at = @At("TAIL"))
    private void wearableToolbox$poseArms(LivingEntity entity, float limbSwing, float limbSwingAmount,
                                          float ageInTicks, float netHeadYaw, float headPitch,
                                          CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!(entity instanceof LocalPlayer player) || player != minecraft.player) {
            return;
        }
        if (!WearableToolboxClient.isToolboxScreenOpen()) {
            return;
        }

        boolean poseArms = WearableToolboxConfig.poseArms();
        boolean lowerHead = WearableToolboxConfig.lowerHead();
        if (!poseArms && !lowerHead) {
            return;
        }

        PlayerModel<?> model = (PlayerModel<?>) (Object) this;

        if (poseArms) {
            // 参考 WATUT（What Are They Up To）打开 GUI 时的姿势：
            // 左臂 -70° / +25°，右臂取其界面中心附近的 -67.5° / -15°；
            // 这里只取静态角度，不跟随鼠标位置。
            float rightX = (float) -Math.toRadians(67.5);
            float leftX = (float) -Math.toRadians(70.0);
            float rightY = (float) -Math.toRadians(15.0);
            float leftY = (float) Math.toRadians(25.0);

            // 行走时的自然反向摆动，加上站立时的轻微呼吸摆动。
            float rightSwing = Mth.cos(limbSwing * 0.6662F) * 0.06F * limbSwingAmount;
            float leftSwing = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 0.06F * limbSwingAmount;
            float breathe = Mth.sin(ageInTicks * 0.12F) * 0.025F;
            float sideSway = Mth.cos(ageInTicks * 0.09F) * 0.015F;

            poseArm(model.rightArm, model.rightSleeve,
                    rightX + rightSwing + breathe,
                    rightY + sideSway,
                    0.0F);
            poseArm(model.leftArm, model.leftSleeve,
                    leftX + leftSwing + breathe,
                    leftY - sideSway,
                    0.0F);
        }

        if (lowerHead) {
            // 低头看界面；朝身体正前方，不跟随视角左右转动。
            // hat 是独立部件，需要同步俯仰与朝向。
            float headDown = (float) Math.toRadians(40.0);
            model.head.xRot = headDown;
            model.head.yRot = 0.0F;
            model.head.zRot = 0.0F;
            model.hat.xRot = headDown;
            model.hat.yRot = 0.0F;
            model.hat.zRot = 0.0F;
        }
    }

    @Unique
    private static void poseArm(ModelPart arm, ModelPart sleeve, float xRot, float yRot, float zRot) {
        arm.xRot = xRot;
        arm.yRot = yRot;
        arm.zRot = zRot;
        sleeve.xRot = xRot;
        sleeve.yRot = yRot;
        sleeve.zRot = zRot;
    }
}
