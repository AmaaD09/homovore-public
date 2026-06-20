package dev.leonetic.features.modules.render;

import dev.leonetic.event.impl.render.Render3DEvent;
import dev.leonetic.event.impl.render.RenderBlockOutlineEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.modules.Module;
import dev.leonetic.features.settings.Setting;
import dev.leonetic.util.render.RenderUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.awt.*;

public class BlockHighlightModule extends Module {
    public Setting<Color> color = color("Color", 255, 0, 0, 255);
    public Setting<Float> lineWidth = num("LineWidth", 1.0f, 0.1f, 5.0f);
    private static final float ANIM_DURATION = 60f;

    private enum State { IDLE, FADING_IN, VISIBLE, SHIFTING, FADING_OUT }

    private State state = State.IDLE;
    private AABB fromBox;
    private AABB toBox;
    private float progress = 0f;
    private BlockPos lastPos;
    private long lastNanos;

    public BlockHighlightModule() {
        super("BlockHighlight", "Draws box at the block that you are looking at", Category.RENDER);
    }

    @Override
    public void onEnable() {
        state = State.IDLE;
        fromBox = null;
        toBox = null;
        progress = 0f;
        lastPos = null;
        lastNanos = 0;
    }

    @Subscribe
    public void onRender3D(Render3DEvent event) {
        long now = System.nanoTime();
        float dt = lastNanos == 0 ? 0f : (now - lastNanos) / 1_000_000f;
        lastNanos = now;

        float duration = ANIM_DURATION;

        BlockPos targetPos = null;
        AABB targetBox = null;
        if (mc.hitResult instanceof BlockHitResult result) {
            BlockPos bp = result.getBlockPos();
            VoxelShape shape = mc.level.getBlockState(bp).getShape(mc.level, bp);
            if (!shape.isEmpty()) {
                AABB box = shape.bounds().move(bp);
                if (mc.player == null || !mc.player.getBoundingBox().intersects(box)) {
                    targetPos = bp;
                    targetBox = box;
                }
            }
        }

        boolean targetChanged = targetPos != null && !targetPos.equals(lastPos);
        boolean targetLost = targetPos == null && lastPos != null;
        lastPos = targetPos;

        switch (state) {
            case IDLE -> {
                if (targetBox != null) {
                    fromBox = targetBox;
                    toBox = targetBox;
                    progress = 0f;
                    state = State.FADING_IN;
                }
            }
            case FADING_IN -> {
                progress = Math.min(1f, progress + dt / duration);
                if (targetChanged) {
                    fromBox = animatedBox();
                    toBox = targetBox;
                    progress = 0f;
                    state = State.SHIFTING;
                } else if (targetLost) {

                    progress = 1f - progress;
                    state = State.FADING_OUT;
                } else if (progress >= 1f) {
                    state = State.VISIBLE;
                }
            }
            case VISIBLE -> {
                progress = 1f;
                if (targetChanged) {
                    fromBox = toBox;
                    toBox = targetBox;
                    progress = 0f;
                    state = State.SHIFTING;
                } else if (targetLost) {
                    progress = 0f;
                    state = State.FADING_OUT;
                }
            }
            case SHIFTING -> {
                progress = Math.min(1f, progress + dt / duration);
                if (targetChanged) {
                    fromBox = animatedBox();
                    toBox = targetBox;
                    progress = 0f;
                } else if (targetLost) {
                    fromBox = animatedBox();
                    toBox = fromBox;
                    progress = 0f;
                    state = State.FADING_OUT;
                } else if (progress >= 1f) {
                    state = State.VISIBLE;
                }
            }
            case FADING_OUT -> {
                progress = Math.min(1f, progress + dt / duration);
                if (targetBox != null) {
                    fromBox = targetBox;
                    toBox = targetBox;
                    progress = 0f;
                    state = State.FADING_IN;
                } else if (progress >= 1f) {
                    state = State.IDLE;
                    fromBox = null;
                    toBox = null;
                }
            }
        }

        if (state == State.IDLE) return;

        float alpha = switch (state) {
            case FADING_IN  -> smoothstep(progress);
            case FADING_OUT -> 1f - smoothstep(progress);
            default         -> 1f;
        };

        float scale = state == State.SHIFTING
            ? 1f - 0.2f * (float) Math.sin(progress * Math.PI)
            : 1f;

        Color c = color.getValue();
        Color drawColor = new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.round(c.getAlpha() * alpha));
        RenderUtil.drawBox(event.getMatrix(), scaledBox(animatedBox(), scale), drawColor, lineWidth.getValue());
    }

    private AABB animatedBox() {
        float t = smoothstep(progress);
        return new AABB(
            lerp(fromBox.minX, toBox.minX, t),
            lerp(fromBox.minY, toBox.minY, t),
            lerp(fromBox.minZ, toBox.minZ, t),
            lerp(fromBox.maxX, toBox.maxX, t),
            lerp(fromBox.maxY, toBox.maxY, t),
            lerp(fromBox.maxZ, toBox.maxZ, t)
        );
    }

    private AABB scaledBox(AABB box, float scale) {
        if (scale == 1f) return box;
        double cx = (box.minX + box.maxX) / 2;
        double cy = (box.minY + box.maxY) / 2;
        double cz = (box.minZ + box.maxZ) / 2;
        double hw = (box.maxX - box.minX) / 2 * scale;
        double hh = (box.maxY - box.minY) / 2 * scale;
        double hd = (box.maxZ - box.minZ) / 2 * scale;
        return new AABB(cx - hw, cy - hh, cz - hd, cx + hw, cy + hh, cz + hd);
    }

    private float smoothstep(float t) {
        return t * t * (3f - 2f * t);
    }

    private double lerp(double a, double b, float t) {
        return a + (b - a) * t;
    }

    @Subscribe
    public void onRenderBlockOutline(RenderBlockOutlineEvent event) {
        event.cancel();
    }
}
