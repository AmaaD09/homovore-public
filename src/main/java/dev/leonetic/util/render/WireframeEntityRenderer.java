package dev.leonetic.util.render;

import com.mojang.blaze3d.vertex.*;
import dev.leonetic.util.traits.Util;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.player.PlayerModel;
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
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public final class WireframeEntityRenderer implements Util {

    private static final int FULL_BRIGHT = 0xF000F0;

    private WireframeEntityRenderer() {}

    public static List<float[][]> render(
            PoseStack eventStack,
            Entity entity,
            Vec3 pos,
            List<float[][]> quads,
            float partialTick,
            Color sideColor,
            Color lineColor,
            float lineWidth) {

        if (quads == null) {
            quads = captureGeometry(entity, partialTick);
        }
        if (quads.isEmpty()) return quads;

        Vec3 cam = mc.getEntityRenderDispatcher().camera.position();
        float dx = (float) (pos.x - cam.x());
        float dy = (float) (pos.y - cam.y());
        float dz = (float) (pos.z - cam.z());

        eventStack.pushPose();
        eventStack.translate(dx, dy, dz);
        PoseStack.Pose pose = eventStack.last();

        if (sideColor.getAlpha() > 0) {
            BufferBuilder fill = Tesselator.getInstance()
                    .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (float[][] q : quads) {
                for (float[] v : q) {
                    fill.addVertex(pose, v[0], v[1], v[2]).setColor(sideColor.getRGB());
                }
            }
            Layers.quads().draw(fill.buildOrThrow());
        }

        BufferBuilder edges = Tesselator.getInstance()
                .begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_LINE_WIDTH);
        for (float[][] q : quads) {
            for (int i = 0; i < 4; i++) {
                float[] v1 = q[i];
                float[] v2 = q[(i + 1) % 4];
                edges.addVertex(pose, v1[0], v1[1], v1[2]).setColor(lineColor.getRGB()).setLineWidth(lineWidth);
                edges.addVertex(pose, v2[0], v2[1], v2[2]).setColor(lineColor.getRGB()).setLineWidth(lineWidth);
            }
        }
        Layers.lines().draw(edges.buildOrThrow());

        eventStack.popPose();
        return quads;
    }

    private static List<float[][]> captureGeometry(Entity entity, float partialTick) {
        try {
            EntityRenderState state = mc.getEntityRenderDispatcher().extractEntity(entity, partialTick);

            CameraRenderState camera = new CameraRenderState();
            camera.pos = Vec3.ZERO;
            camera.entityPos = Vec3.ZERO;
            camera.blockPos = BlockPos.ZERO;
            camera.orientation = new Quaternionf();
            camera.initialized = true;

            CapturingCollector collector = new CapturingCollector();
            mc.getEntityRenderDispatcher().submit(state, camera, 0.0, 0.0, 0.0, new PoseStack(), collector);
            return collector.consumer.quads;
        } catch (Exception ignored) {
            return new ArrayList<>();
        }
    }

    private static final class CapturingCollector implements SubmitNodeCollector {
        final CapturingConsumer consumer = new CapturingConsumer();

        @Override
        public OrderedSubmitNodeCollector order(int order) { return this; }

        @Override
        public <S> void submitModel(Model<? super S> model, S modelState, PoseStack poseStack,
                RenderType renderType, int packedLight, int overlay, int blockPosition,
                TextureAtlasSprite sprite, int flags, ModelFeatureRenderer.CrumblingOverlay crumbling) {
            if (model instanceof HumanoidModel<?> humanoid) {
                renderFirstLayerOnly(humanoid, poseStack, packedLight, overlay);
            } else {
                model.renderToBuffer(poseStack, consumer, packedLight, overlay);
            }
        }

        @Override
        public void submitModelPart(ModelPart part, PoseStack poseStack, RenderType renderType,
                int packedLight, int overlay, TextureAtlasSprite sprite,
                boolean copyNormals, boolean fullBright, int flags,
                ModelFeatureRenderer.CrumblingOverlay crumbling, int data) {
            part.render(poseStack, consumer, packedLight, overlay);
        }

        private void renderFirstLayerOnly(HumanoidModel<?> model, PoseStack poseStack, int light, int overlay) {

            boolean hatVis = model.hat.visible;
            model.hat.visible = false;

            boolean jacketVis = false, lSleeveVis = false, rSleeveVis = false,
                    lPantsVis = false, rPantsVis = false;
            if (model instanceof PlayerModel pm) {
                jacketVis  = pm.jacket.visible;      pm.jacket.visible      = false;
                lSleeveVis = pm.leftSleeve.visible;  pm.leftSleeve.visible  = false;
                rSleeveVis = pm.rightSleeve.visible; pm.rightSleeve.visible = false;
                lPantsVis  = pm.leftPants.visible;   pm.leftPants.visible   = false;
                rPantsVis  = pm.rightPants.visible;  pm.rightPants.visible  = false;
            }

            model.renderToBuffer(poseStack, consumer, light, overlay);

            model.hat.visible = hatVis;
            if (model instanceof PlayerModel pm) {
                pm.jacket.visible      = jacketVis;
                pm.leftSleeve.visible  = lSleeveVis;
                pm.rightSleeve.visible = rSleeveVis;
                pm.leftPants.visible   = lPantsVis;
                pm.rightPants.visible  = rPantsVis;
            }
        }

        @Override public void submitShadow(PoseStack p, float r, List<EntityRenderState.ShadowPiece> s) {}
        @Override public void submitNameTag(PoseStack p, Vec3 pos, int bg, Component text, boolean player, int color, double dist, CameraRenderState cam) {}
        @Override public void submitText(PoseStack p, float x, float y, FormattedCharSequence text, boolean shadow, Font.DisplayMode mode, int bg, int fg, int light, int lh) {}
        @Override public void submitFlame(PoseStack p, EntityRenderState s, Quaternionf q) {}
        @Override public void submitLeash(PoseStack p, EntityRenderState.LeashState l) {}
        @Override public void submitBlock(PoseStack p, BlockState b, int x, int y, int z) {}
        @Override public void submitMovingBlock(PoseStack p, MovingBlockRenderState m) {}
        @Override public void submitBlockModel(PoseStack p, RenderType r, BlockStateModel m, float a, float b, float c, int l, int o, int f) {}
        @Override public void submitItem(PoseStack p, ItemDisplayContext ctx, int l1, int l2, int l3, int[] tints, List<BakedQuad> quads, RenderType r, ItemStackRenderState.FoilType foil) {}
        @Override public void submitCustomGeometry(PoseStack p, RenderType r, SubmitNodeCollector.CustomGeometryRenderer geom) {}
        @Override public void submitParticleGroup(SubmitNodeCollector.ParticleGroupRenderer renderer) {}
    }

    private static final class CapturingConsumer implements VertexConsumer {
        final List<float[][]> quads = new ArrayList<>();
        private final float[][] pending = new float[4][];
        private int count = 0;

        private void pushVertex(float x, float y, float z) {
            pending[count++] = new float[]{x, y, z};
            if (count == 4) {
                quads.add(new float[][]{
                        pending[0].clone(), pending[1].clone(),
                        pending[2].clone(), pending[3].clone()
                });
                count = 0;
            }
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            pushVertex(x, y, z);
            return this;
        }

        @Override public VertexConsumer setColor(int r, int g, int b, int a) { return this; }
        @Override public VertexConsumer setColor(int argb) { return this; }
        @Override public VertexConsumer setUv(float u, float v) { return this; }
        @Override public VertexConsumer setUv1(int u, int v) { return this; }
        @Override public VertexConsumer setUv2(int u, int v) { return this; }
        @Override public VertexConsumer setNormal(float nx, float ny, float nz) { return this; }
        @Override public VertexConsumer setLineWidth(float w) { return this; }
    }
}
