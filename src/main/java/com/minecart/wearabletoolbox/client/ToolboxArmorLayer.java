package com.minecart.wearabletoolbox.client;

import com.minecart.wearabletoolbox.WearableToolbox;
import com.minecart.wearabletoolbox.common.ToolboxItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.ModelState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 把 assets/wearable_toolbox/models/brown.json 作为胸甲栏工具箱的展示模型。
 *
 * <p>该 JSON 是 Blockbench 的 Java 方块模型，不是实体模型。这里在客户端首次渲染时读取它，
 * 用 {@link BlockModel#bakeFace} 烘成 {@link BakedQuad}，再挂到玩家 body 上；
 * 这样能保留 Blockbench 里逐面的 UV 与元素旋转。以后换模型只需替换 JSON 或调整下面的变换常量。</p>
 */
public final class ToolboxArmorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation MODEL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(WearableToolbox.MODID, "models/brown.json");
    private static final ModelState MODEL_STATE = new ModelState() {
    };

    /**
     * 模型在玩家背部的摆放参数。模型坐标 16 单位 = 1 格。
     * 先平移到背部，再绕 Y 轴转 180°，缩放后把模型 X/Z 中心移到原点。
     */
    private static final float BACK_Y = 0.50F;
    private static final float BACK_Z = 0.45F;
    private static final float SCALE = 0.55F;
    private static final float CENTER_X = 0.50F;
    private static final float CENTER_Z = 0.6875F;

    private static List<BakedQuad> quads;
    private static boolean loadFailed;

    public ToolboxArmorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (!ToolboxItems.isToolbox(chest)) {
            return;
        }

        List<BakedQuad> modelQuads = getQuads();
        if (modelQuads.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        getParentModel().body.translateAndRotate(poseStack);
        poseStack.translate(0.0F, BACK_Y, BACK_Z);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.scale(SCALE, SCALE, SCALE);
        poseStack.translate(-CENTER_X, 0.0F, -CENTER_Z);

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose pose = poseStack.last();
        for (BakedQuad quad : modelQuads) {
            consumer.putBulkData(pose, quad, 1.0F, 1.0F, 1.0F, 1.0F, packedLight, OverlayTexture.NO_OVERLAY);
        }
        poseStack.popPose();
    }

    private static List<BakedQuad> getQuads() {
        if (quads != null) {
            return quads;
        }
        if (loadFailed) {
            return List.of();
        }

        try {
            Resource resource = Minecraft.getInstance().getResourceManager()
                    .getResource(MODEL_LOCATION)
                    .orElseThrow();
            BlockModel model;
            try (InputStream stream = resource.open()) {
                model = BlockModel.fromStream(new InputStreamReader(stream, StandardCharsets.UTF_8));
            }

            List<BakedQuad> baked = new ArrayList<>();
            for (BlockElement element : model.getElements()) {
                for (var entry : element.faces.entrySet()) {
                    BlockElementFace face = entry.getValue();
                    TextureAtlasSprite sprite = model.getMaterial(face.texture()).sprite();
                    baked.add(BlockModel.bakeFace(element, face, sprite, entry.getKey(), MODEL_STATE));
                }
            }
            quads = List.copyOf(baked);
        } catch (Exception exception) {
            loadFailed = true;
            WearableToolbox.LOGGER.error("无法加载可穿戴工具箱模型 {}", MODEL_LOCATION, exception);
            quads = List.of();
        }
        return quads;
    }
}
