package dev.leonetic.util.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.List;

public final class HandSilhouetteCollector implements OrderedSubmitNodeCollector {

    private final OutlineBufferSource outline;
    private final int color;

    public HandSilhouetteCollector(OutlineBufferSource outline, int rgb) {
        this.outline = outline;
        this.color = 0xFF000000 | rgb;
    }

    @Override
    public <S> void submitModel(Model<? super S> model, S state, PoseStack poseStack,
                                RenderType renderType, int light, int overlay, int tintColor,
                                TextureAtlasSprite sprite, int outlineColor,
                                ModelFeatureRenderer.CrumblingOverlay crumbling) {
        VertexConsumer consumer = buffer(renderType, sprite);
        if (consumer == null) return;
        try {
            model.setupAnim(state);
            model.renderToBuffer(poseStack, consumer, light, overlay, tintColor);
        } catch (Exception ignored) {

        }
    }

    @Override
    public void submitModelPart(ModelPart part, PoseStack poseStack, RenderType renderType,
                                int light, int overlay, TextureAtlasSprite sprite,
                                boolean copyNormals, boolean fullBright, int tintColor,
                                ModelFeatureRenderer.CrumblingOverlay crumbling, int outlineColor) {
        VertexConsumer consumer = buffer(renderType, sprite);
        if (consumer == null) return;
        try {
            part.render(poseStack, consumer, light, overlay, tintColor);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void submitItem(PoseStack poseStack, ItemDisplayContext context, int light, int overlay,
                           int outlineColor, int[] tintLayers, List<BakedQuad> quads,
                           RenderType renderType, ItemStackRenderState.FoilType foil) {
        VertexConsumer consumer = buffer(renderType, null);
        if (consumer == null) return;
        PoseStack.Pose pose = poseStack.last();
        for (BakedQuad quad : quads) {
            consumer.putBulkData(pose, quad, 1f, 1f, 1f, 1f, light, overlay);
        }
    }

    private VertexConsumer buffer(RenderType renderType, TextureAtlasSprite sprite) {
        if (HandShaderRender.capturePaused) return null;
        VertexConsumer consumer;
        try {
            outline.setColor(color);
            consumer = outline.getBuffer(renderType);
        } catch (IllegalStateException noOutline) {
            return null;
        }
        return sprite != null ? sprite.wrap(consumer) : consumer;
    }

    @Override public void submitShadow(PoseStack p, float f, List<EntityRenderState.ShadowPiece> s) {}
    @Override public void submitNameTag(PoseStack p, Vec3 v, int i, Component c, boolean b, int j, double d, CameraRenderState s) {}
    @Override public void submitText(PoseStack p, float f, float g, FormattedCharSequence t, boolean b, Font.DisplayMode m, int i, int j, int k, int l) {}
    @Override public void submitFlame(PoseStack p, EntityRenderState s, Quaternionf q) {}
    @Override public void submitLeash(PoseStack p, EntityRenderState.LeashState s) {}
    @Override public void submitBlock(PoseStack p, BlockState s, int i, int j, int k) {}
    @Override public void submitMovingBlock(PoseStack p, MovingBlockRenderState s) {}
    @Override public void submitBlockModel(PoseStack p, RenderType r, BlockStateModel m, float f, float g, float h, int i, int j, int k) {}
    @Override public void submitCustomGeometry(PoseStack p, RenderType r, SubmitNodeCollector.CustomGeometryRenderer renderer) {}
    @Override public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer renderer) {}
}
