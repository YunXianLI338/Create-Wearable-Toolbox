package com.minecart.wearabletoolbox.client;

import com.minecart.wearabletoolbox.WearableToolbox;
import com.minecart.wearabletoolbox.common.ToolboxItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/**
 * 临时占位：胸甲栏装着工具箱时，借用原版锁链甲的 body 模型渲染。
 * 后续替换成工具箱专用模型时，只需要改 {@link #LAYER} 与纹理。
 */
public final class ToolboxArmorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(WearableToolbox.MODID, "toolbox_armor"),
            "main"
    );

    /**
     * 原版锁链甲外层纹理，64x32。
     */
    private static final ResourceLocation CHAINMAIL_TEXTURE =
            ResourceLocation.withDefaultNamespace("textures/models/armor/chainmail_layer_1.png");

    private final HumanoidModel<AbstractClientPlayer> model;

    public ToolboxArmorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent,
                             EntityModelSet models) {
        super(parent);
        this.model = new HumanoidModel<>(models.bakeLayer(LAYER));
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!ToolboxItems.isToolbox(chest)) {
            return;
        }

        getParentModel().copyPropertiesTo(model);
        model.setAllVisible(false);
        model.body.visible = true;

        VertexConsumer consumer = buffer.getBuffer(RenderType.armorCutoutNoCull(CHAINMAIL_TEXTURE));
        model.renderToBuffer(poseStack, consumer, packedLight, OverlayTexture.NO_OVERLAY, -1);
    }
}
