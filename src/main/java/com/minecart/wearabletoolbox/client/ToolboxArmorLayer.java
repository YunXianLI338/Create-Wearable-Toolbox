package com.minecart.wearabletoolbox.client;

import com.minecart.wearabletoolbox.WearableToolbox;
import com.minecart.wearabletoolbox.common.EquippedToolbox;
import com.minecart.wearabletoolbox.common.WearableToolboxMenu;
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
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 把 assets/wearable_toolbox/models/brown.json 作为身上工具箱的展示模型。
 *
 * <p>该 JSON 是 Blockbench 的 Java 方块模型，不是实体模型。这里在客户端首次渲染时读取它，
 * 用 {@link BlockModel#bakeFace} 烘成 {@link BakedQuad}，再挂到玩家 body 上；
 * 这样能保留 Blockbench 里逐面的 UV 与元素旋转。打开工具箱界面时使用 {@code brown_on.json}。</p>
 */
public final class ToolboxArmorLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ResourceLocation MODEL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(WearableToolbox.MODID, "models/brown.json");
    private static final ResourceLocation OPEN_MODEL_LOCATION =
            ResourceLocation.fromNamespaceAndPath(WearableToolbox.MODID, "models/brown_on.json");
    private static final ModelState MODEL_STATE = new ModelState() {
    };

    /** 模型中心的 X 坐标。玩家右方是负 X。 */
    private static final float FRONT_X = -0.6F / 16.0F;

    /** 模型包围盒下沿的 Y 坐标。实体 Y 轴向下，减去高度即视觉上移。 */
    private static final float FRONT_Y = 0.50F - 8.9F / 16.0F;

    /** 模型中心的 Z 坐标。负 Z 是玩家前方，-0.10 让箱体贴在胸前。 */
    private static final float FRONT_Z = -0.10F + 0.8F / 16.0F;

    /** 整体缩放。 */
    private static final float SCALE = 0.48F;

    /** 模型原始坐标的 X 中心（8/16 格）。 */
    private static final float CENTER_X = 0.50F;

    /** 模型原始坐标的 Z 中心（11/16 格）。 */
    private static final float CENTER_Z = 0.6875F;

    /** 模型原始高度：JSON 中 y=0 到 y=23，共 23/16 格。 */
    private static final float MODEL_HEIGHT = 23.0F / 16.0F;

    private static List<BakedQuad> quads;
    private static List<BakedQuad> openQuads;
    private static boolean normalLoadFailed;
    private static boolean openLoadFailed;

    public ToolboxArmorLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        EquippedToolbox equipped = EquippedToolbox.find(player);
        if (equipped == null) {
            return;
        }

        boolean open = isToolboxOpen(player);
        renderModel(poseStack, buffer, packedLight, getParentModel().body, open, false);
    }

    /**
     * 把工具箱模型挂到任意 HumanoidModel 的 body 上；玩家与盔甲架共用。
     *
     * @param open         是否使用打开盖子的模型
     * @param flipVertical 是否再绕模型中心上下翻转；盔甲架的 body 坐标系与玩家不同，需要翻转
     */
    public static void renderModel(PoseStack poseStack, MultiBufferSource buffer, int packedLight,
                                   net.minecraft.client.model.geom.ModelPart body, boolean open,
                                   boolean flipVertical) {
        List<BakedQuad> modelQuads = getQuads(open);
        if (modelQuads.isEmpty()) {
            return;
        }

        poseStack.pushPose();
        body.translateAndRotate(poseStack);
        float halfHeight = MODEL_HEIGHT * SCALE / 2.0F;
        poseStack.translate(FRONT_X, FRONT_Y + halfHeight, FRONT_Z);
        poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        poseStack.scale(SCALE, SCALE, SCALE);
        poseStack.translate(-CENTER_X, -MODEL_HEIGHT / 2.0F, -CENTER_Z);
        if (flipVertical) {
            // 此时原点已经在模型中心，再转 180° 只改变上下/左右朝向，不会把模型甩到别处。
            poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
        }

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        PoseStack.Pose pose = poseStack.last();
        for (BakedQuad quad : modelQuads) {
            consumer.putBulkData(pose, quad, 1.0F, 1.0F, 1.0F, 1.0F, packedLight, OverlayTexture.NO_OVERLAY);
        }
        poseStack.popPose();
    }

    /**
     * V 打开的 GUI 或 Alt 径向菜单打开时，换成打开盖子的模型。
     */
    private static boolean isToolboxOpen(AbstractClientPlayer player) {
        if (player.containerMenu instanceof WearableToolboxMenu) {
            return true;
        }
        return Minecraft.getInstance().screen instanceof WearableToolboxRadialScreen;
    }

    private static List<BakedQuad> getQuads(boolean open) {
        if (!open || openLoadFailed) {
            return getNormalQuads();
        }
        if (openQuads == null) {
            openQuads = loadQuads(OPEN_MODEL_LOCATION, true);
            if (openQuads.isEmpty()) {
                openLoadFailed = true;
                return getNormalQuads();
            }
        }
        return openQuads;
    }

    private static List<BakedQuad> getNormalQuads() {
        if (quads != null) {
            return quads;
        }
        if (normalLoadFailed) {
            return List.of();
        }
        quads = loadQuads(MODEL_LOCATION, false);
        if (quads.isEmpty()) {
            normalLoadFailed = true;
        }
        return quads;
    }

    private static List<BakedQuad> loadQuads(ResourceLocation location, boolean optional) {
        try {
            Resource resource = Minecraft.getInstance().getResourceManager()
                    .getResource(location)
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
            return List.copyOf(baked);
        } catch (Exception exception) {
            if (!optional) {
                WearableToolbox.LOGGER.error("无法加载可穿戴工具箱模型 {}", location, exception);
            }
            return List.of();
        }
    }
}
