package dev.leonetic.util.render;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.resource.CrossFrameResourcePool;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.PostChain;

public final class HandShaderRender {

    private static TextureTarget handTarget;
    private static int width, height;

    private static OutlineBufferSource outline;
    private static boolean captureOpen;
    private static boolean hasCapture;

    private static final CrossFrameResourcePool RESOURCE_POOL = new CrossFrameResourcePool(3);

    public static boolean capturePaused;

    private HandShaderRender() {}

    private static boolean ensureTarget() {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth();
        int h = mc.getWindow().getHeight();
        if (w <= 0 || h <= 0) return false;

        if (handTarget == null) {
            handTarget = new TextureTarget("homovore_hand_shader", w, h, true);
            width = w;
            height = h;
        } else if (w != width || h != height) {
            handTarget.resize(w, h);
            width = w;
            height = h;
        }
        return true;
    }

    public static HandSilhouetteCollector beginCapture(int rgb) {
        if (!ensureTarget()) return null;

        RenderSystem.getDevice().createCommandEncoder()
                .clearColorAndDepthTextures(handTarget.getColorTexture(), 0,
                        handTarget.getDepthTexture(), 1.0);

        if (outline == null) outline = new OutlineBufferSource();
        captureOpen = true;
        hasCapture = false;
        return new HandSilhouetteCollector(outline, rgb);
    }

    public static void flush() {
        if (!captureOpen || outline == null || handTarget == null) return;

        RenderSystem.outputColorTextureOverride = handTarget.getColorTextureView();
        RenderSystem.outputDepthTextureOverride = handTarget.getDepthTextureView();
        try {
            outline.endOutlineBatch();
            hasCapture = true;
        } finally {
            RenderSystem.outputColorTextureOverride = null;
            RenderSystem.outputDepthTextureOverride = null;
            captureOpen = false;
        }
    }

    public static void composite(PostChain chain) {
        if (!hasCapture || handTarget == null || chain == null) return;
        hasCapture = false;

        chain.process(handTarget, RESOURCE_POOL);
        handTarget.blitAndBlendToTexture(Minecraft.getInstance().getMainRenderTarget().getColorTextureView());
        RESOURCE_POOL.endFrame();
    }
}
