package dev.leonetic.util.render;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.leonetic.util.render.state.RectRenderState;
import dev.leonetic.util.traits.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2f;

import java.awt.*;

public class RenderUtil implements Util {

    public static void rect(GuiGraphics context, float x1, float y1, float x2, float y2, int color) {
        int ix1 = Math.round(x1);
        int iy1 = Math.round(y1);
        int ix2 = Math.round(x2);
        int iy2 = Math.round(y2);
        context.fill(ix1, iy1, ix2, iy2, GuiFade.apply(color));
    }

    public static void rect(GuiGraphics context, float x1, float y1, float x2, float y2, int color, float width) {
        int w = Math.max(1, Math.round(width));
        int c = GuiFade.apply(color);
        context.fill(Math.round(x1), Math.round(y1), Math.round(x2), Math.round(y1) + w, c);
        context.fill(Math.round(x2) - w, Math.round(y1), Math.round(x2), Math.round(y2), c);
        context.fill(Math.round(x1), Math.round(y2) - w, Math.round(x2), Math.round(y2), c);
        context.fill(Math.round(x1), Math.round(y1), Math.round(x1) + w, Math.round(y2), c);
    }

    public static void horizontalGradient(GuiGraphics context, float x1, float y1, float x2, float y2, Color left, Color right) {
        int ix1 = Math.round(x1);
        int iy1 = Math.round(y1);
        int ix2 = Math.round(x2);
        int iy2 = Math.round(y2);

        gradient(context, ix1, iy1, ix2, iy2, left.hashCode(), left.hashCode(), right.hashCode(), right.hashCode());
    }

    public static void verticalGradient(GuiGraphics context, float x1, float y1, float x2, float y2, Color top, Color bottom) {
        int ix1 = Math.round(x1);
        int iy1 = Math.round(y1);
        int ix2 = Math.round(x2);
        int iy2 = Math.round(y2);

        gradient(context, ix1, iy1, ix2, iy2, top.hashCode(), bottom.hashCode(), bottom.hashCode(), top.hashCode());
    }

    public static void shadow(GuiGraphics context, float x1, float y1, float x2, float y2, int radius, int alpha) {
        if (radius <= 0 || alpha <= 0) return;
        int ix1 = Math.round(x1);
        int iy1 = Math.round(y1);
        int ix2 = Math.round(x2);
        int iy2 = Math.round(y2);
        int solid = (Math.min(255, alpha) << 24);
        int clear = 0;

        gradient(context, ix1, iy1 - radius, ix2, iy1, clear, solid, solid, clear);

        gradient(context, ix1, iy2, ix2, iy2 + radius, solid, clear, clear, solid);

        gradient(context, ix1 - radius, iy1, ix1, iy2, clear, clear, solid, solid);

        gradient(context, ix2, iy1, ix2 + radius, iy2, solid, solid, clear, clear);

        int cornerAlpha = (int) (alpha * 0.7f) << 24;
        gradient(context, ix1 - radius, iy1 - radius, ix1, iy1, clear, clear, cornerAlpha, clear);
        gradient(context, ix2, iy1 - radius, ix2 + radius, iy1, clear, clear, clear, cornerAlpha);
        gradient(context, ix1 - radius, iy2, ix1, iy2 + radius, clear, cornerAlpha, clear, clear);
        gradient(context, ix2, iy2, ix2 + radius, iy2 + radius, cornerAlpha, clear, clear, clear);
    }

    public static void gradient(GuiGraphics graphics,
                                int x1, int y1, int x2, int y2,
                                int topLeft, int bottomLeft, int bottomRight, int topRight) {
        graphics.guiRenderState.submitGuiElement(new RectRenderState(
                RenderPipelines.GUI, TextureSetup.noTexture(), new Matrix3x2f(graphics.pose()),
                x1, y1, x2, y2,
                GuiFade.apply(topLeft), GuiFade.apply(bottomLeft), GuiFade.apply(bottomRight), GuiFade.apply(topRight),
                graphics.scissorStack.peek()
        ));
    }

    public static void rect(PoseStack stack, float x1, float y1, float x2, float y2, int color) {
        rectFilled(stack, x1, y1, x2, y2, color);
    }

    public static void rect(PoseStack stack, float x1, float y1, float x2, float y2, int color, float width) {
        drawHorizontalLine(stack, x1, x2, y1, color, width);
        drawVerticalLine(stack, x2, y1, y2, color, width);
        drawHorizontalLine(stack, x1, x2, y2, color, width);
        drawVerticalLine(stack, x1, y1, y2, color, width);
    }

    protected static void drawHorizontalLine(PoseStack matrices, float x1, float x2, float y, int color) {
        if (x2 < x1) {
            float i = x1;
            x1 = x2;
            x2 = i;
        }

        rectFilled(matrices, x1, y, x2 + 1, y + 1, color);
    }

    protected static void drawVerticalLine(PoseStack matrices, float x, float y1, float y2, int color) {
        if (y2 < y1) {
            float i = y1;
            y1 = y2;
            y2 = i;
        }

        rectFilled(matrices, x, y1 + 1, x + 1, y2, color);
    }

    protected static void drawHorizontalLine(PoseStack matrices, float x1, float x2, float y, int color, float width) {
        if (x2 < x1) {
            float i = x1;
            x1 = x2;
            x2 = i;
        }

        rectFilled(matrices, x1, y, x2 + width, y + width, color);
    }

    protected static void drawVerticalLine(PoseStack matrices, float x, float y1, float y2, int color, float width) {
        if (y2 < y1) {
            float i = y1;
            y1 = y2;
            y2 = i;
        }

        rectFilled(matrices, x, y1 + width, x + width, y2, color);
    }

    public static void rectFilled(PoseStack matrix, float x1, float y1, float x2, float y2, int color) {
        float i;
        if (x1 < x2) {
            i = x1;
            x1 = x2;
            x2 = i;
        }

        if (y1 < y2) {
            i = y1;
            y1 = y2;
            y2 = i;
        }

        float f = (float) (color >> 24 & 255) / 255.0F;
        float g = (float) (color >> 16 & 255) / 255.0F;
        float h = (float) (color >> 8 & 255) / 255.0F;
        float j = (float) (color & 255) / 255.0F;

        BufferBuilder bufferBuilder = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.addVertex(matrix.last().pose(), x1, y2, 0.0F).setColor(g, h, j, f);
        bufferBuilder.addVertex(matrix.last().pose(), x2, y2, 0.0F).setColor(g, h, j, f);
        bufferBuilder.addVertex(matrix.last().pose(), x2, y1, 0.0F).setColor(g, h, j, f);
        bufferBuilder.addVertex(matrix.last().pose(), x1, y1, 0.0F).setColor(g, h, j, f);

        Layers.quads().draw(bufferBuilder.buildOrThrow());
    }

    public static void horizontalGradient(PoseStack matrix, float x1, float y1, float x2, float y2, Color left, Color right) {
        BufferBuilder bufferBuilder = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.addVertex(matrix.last().pose(), x1, y1, 0.0F).setColor(left.getRed() / 255.0F, left.getGreen() / 255.0F, left.getBlue() / 255.0F, left.getAlpha() / 255.0F);
        bufferBuilder.addVertex(matrix.last().pose(), x1, y2, 0.0F).setColor(left.getRed() / 255.0F, left.getGreen() / 255.0F, left.getBlue() / 255.0F, left.getAlpha() / 255.0F);
        bufferBuilder.addVertex(matrix.last().pose(), x2, y2, 0.0F).setColor(right.getRed() / 255.0F, right.getGreen() / 255.0F, right.getBlue() / 255.0F, right.getAlpha() / 255.0F);
        bufferBuilder.addVertex(matrix.last().pose(), x2, y1, 0.0F).setColor(right.getRed() / 255.0F, right.getGreen() / 255.0F, right.getBlue() / 255.0F, right.getAlpha() / 255.0F);

        Layers.quads().draw(bufferBuilder.buildOrThrow());
    }

    public static void verticalGradient(PoseStack matrix, float x1, float y1, float x2, float y2, Color top, Color bottom) {
        BufferBuilder bufferBuilder = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        bufferBuilder.addVertex(matrix.last().pose(), x1, y1, 0.0F).setColor(top.getRed() / 255.0F, top.getGreen() / 255.0F, top.getBlue() / 255.0F, top.getAlpha() / 255.0F);
        bufferBuilder.addVertex(matrix.last().pose(), x1, y2, 0.0F).setColor(bottom.getRed() / 255.0F, bottom.getGreen() / 255.0F, bottom.getBlue() / 255.0F, bottom.getAlpha() / 255.0F);
        bufferBuilder.addVertex(matrix.last().pose(), x2, y2, 0.0F).setColor(bottom.getRed() / 255.0F, bottom.getGreen() / 255.0F, bottom.getBlue() / 255.0F, bottom.getAlpha() / 255.0F);
        bufferBuilder.addVertex(matrix.last().pose(), x2, y1, 0.0F).setColor(top.getRed() / 255.0F, top.getGreen() / 255.0F, top.getBlue() / 255.0F, top.getAlpha() / 255.0F);

        Layers.quads().draw(bufferBuilder.buildOrThrow());
    }

    public static void drawBoxFilled(PoseStack stack, AABB box, Color c) {
        Gizmos.cuboid(box, GizmoStyle.fill(c.getRGB())).setAlwaysOnTop();
    }

    public static void drawBoxFilled(PoseStack stack, Vec3 vec, Color c) {
        drawBoxFilled(stack, AABB.unitCubeFromLowerCorner(vec), c);
    }

    public static void drawBoxFilled(PoseStack stack, BlockPos bp, Color c) {
        drawBoxFilled(stack, new AABB(bp), c);
    }

    public static void drawBox(PoseStack stack, AABB box, Color c, float lineWidth) {
        Gizmos.cuboid(box, GizmoStyle.stroke(c.getRGB(), lineWidth)).setAlwaysOnTop();
    }

    public static void drawBox(PoseStack stack, Vec3 vec, Color c, float lineWidth) {
        drawBox(stack, AABB.unitCubeFromLowerCorner(vec), c, lineWidth);
    }

    public static void drawBox(PoseStack stack, BlockPos bp, Color c, float lineWidth) {
        drawBox(stack, new AABB(bp), c, lineWidth);
    }

    public static void drawLine(Vec3 from, Vec3 to, Color c, float lineWidth) {
        Gizmos.line(from, to, c.getRGB(), lineWidth).setAlwaysOnTop();
    }

    public static PoseStack matrixFrom(Vec3 pos) {
        PoseStack matrices = new PoseStack();
        Camera camera = mc.gameRenderer.getMainCamera();
        matrices.mulPose(Axis.XP.rotationDegrees(camera.xRot()));
        matrices.mulPose(Axis.YP.rotationDegrees(camera.yRot() + 180.0F));
        matrices.translate(pos.x() - camera.position().x, pos.y() - camera.position().y, pos.z() - camera.position().z);
        return matrices;
    }
}
