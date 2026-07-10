package dev.leonetic.util.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
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

public final class TeeSubmitCollector implements SubmitNodeCollector {

    private final SubmitNodeCollector primary;
    private final OrderedSubmitNodeCollector secondary;
    private final OrderedSubmitNodeCollector tee;

    public TeeSubmitCollector(SubmitNodeCollector primary, OrderedSubmitNodeCollector secondary) {
        this.primary = primary;
        this.secondary = secondary;
        this.tee = new Tee(primary, secondary);
    }

    @Override
    public OrderedSubmitNodeCollector order(int order) {
        return new Tee(primary.order(order), secondary);
    }

    @Override public <S> void submitModel(Model<? super S> m, S s, PoseStack p, RenderType r, int l, int o, int t, TextureAtlasSprite sp, int oc, ModelFeatureRenderer.CrumblingOverlay c) { tee.submitModel(m, s, p, r, l, o, t, sp, oc, c); }
    @Override public void submitModelPart(ModelPart part, PoseStack p, RenderType r, int l, int o, TextureAtlasSprite sp, boolean cn, boolean fb, int t, ModelFeatureRenderer.CrumblingOverlay c, int oc) { tee.submitModelPart(part, p, r, l, o, sp, cn, fb, t, c, oc); }
    @Override public void submitItem(PoseStack p, ItemDisplayContext ctx, int l, int o, int oc, int[] tl, List<BakedQuad> q, RenderType r, ItemStackRenderState.FoilType f) { tee.submitItem(p, ctx, l, o, oc, tl, q, r, f); }
    @Override public void submitShadow(PoseStack p, float f, List<EntityRenderState.ShadowPiece> s) { primary.submitShadow(p, f, s); }
    @Override public void submitNameTag(PoseStack p, Vec3 v, int i, Component c, boolean b, int j, double d, CameraRenderState s) { primary.submitNameTag(p, v, i, c, b, j, d, s); }
    @Override public void submitText(PoseStack p, float f, float g, FormattedCharSequence t, boolean b, Font.DisplayMode m, int i, int j, int k, int l) { primary.submitText(p, f, g, t, b, m, i, j, k, l); }
    @Override public void submitFlame(PoseStack p, EntityRenderState s, Quaternionf q) { primary.submitFlame(p, s, q); }
    @Override public void submitLeash(PoseStack p, EntityRenderState.LeashState s) { primary.submitLeash(p, s); }
    @Override public void submitBlock(PoseStack p, BlockState s, int i, int j, int k) { primary.submitBlock(p, s, i, j, k); }
    @Override public void submitMovingBlock(PoseStack p, MovingBlockRenderState s) { primary.submitMovingBlock(p, s); }
    @Override public void submitBlockModel(PoseStack p, RenderType r, BlockStateModel m, float f, float g, float h, int i, int j, int k) { primary.submitBlockModel(p, r, m, f, g, h, i, j, k); }
    @Override public void submitCustomGeometry(PoseStack p, RenderType r, SubmitNodeCollector.CustomGeometryRenderer renderer) { primary.submitCustomGeometry(p, r, renderer); }
    @Override public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer renderer) { primary.submitParticleGroup(renderer); }

    private record Tee(OrderedSubmitNodeCollector primary, OrderedSubmitNodeCollector secondary)
            implements OrderedSubmitNodeCollector {

        @Override
        public <S> void submitModel(Model<? super S> m, S s, PoseStack p, RenderType r, int l, int o, int t,
                                    TextureAtlasSprite sp, int oc, ModelFeatureRenderer.CrumblingOverlay c) {
            primary.submitModel(m, s, p, r, l, o, t, sp, oc, c);
            secondary.submitModel(m, s, p, r, l, o, t, sp, oc, c);
        }

        @Override
        public void submitModelPart(ModelPart part, PoseStack p, RenderType r, int l, int o, TextureAtlasSprite sp,
                                    boolean cn, boolean fb, int t, ModelFeatureRenderer.CrumblingOverlay c, int oc) {
            primary.submitModelPart(part, p, r, l, o, sp, cn, fb, t, c, oc);
            secondary.submitModelPart(part, p, r, l, o, sp, cn, fb, t, c, oc);
        }

        @Override
        public void submitItem(PoseStack p, ItemDisplayContext ctx, int l, int o, int oc, int[] tl,
                               List<BakedQuad> q, RenderType r, ItemStackRenderState.FoilType f) {
            primary.submitItem(p, ctx, l, o, oc, tl, q, r, f);
            secondary.submitItem(p, ctx, l, o, oc, tl, q, r, f);
        }

        @Override public void submitShadow(PoseStack p, float f, List<EntityRenderState.ShadowPiece> s) { primary.submitShadow(p, f, s); }
        @Override public void submitNameTag(PoseStack p, Vec3 v, int i, Component c, boolean b, int j, double d, CameraRenderState s) { primary.submitNameTag(p, v, i, c, b, j, d, s); }
        @Override public void submitText(PoseStack p, float f, float g, FormattedCharSequence t, boolean b, Font.DisplayMode m, int i, int j, int k, int l) { primary.submitText(p, f, g, t, b, m, i, j, k, l); }
        @Override public void submitFlame(PoseStack p, EntityRenderState s, Quaternionf q) { primary.submitFlame(p, s, q); }
        @Override public void submitLeash(PoseStack p, EntityRenderState.LeashState s) { primary.submitLeash(p, s); }
        @Override public void submitBlock(PoseStack p, BlockState s, int i, int j, int k) { primary.submitBlock(p, s, i, j, k); }
        @Override public void submitMovingBlock(PoseStack p, MovingBlockRenderState s) { primary.submitMovingBlock(p, s); }
        @Override public void submitBlockModel(PoseStack p, RenderType r, BlockStateModel m, float f, float g, float h, int i, int j, int k) { primary.submitBlockModel(p, r, m, f, g, h, i, j, k); }
        @Override public void submitCustomGeometry(PoseStack p, RenderType r, SubmitNodeCollector.CustomGeometryRenderer renderer) { primary.submitCustomGeometry(p, r, renderer); }
        @Override public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer renderer) { primary.submitParticleGroup(renderer); }
    }
}
