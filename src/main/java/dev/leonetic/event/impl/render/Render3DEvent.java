package dev.leonetic.event.impl.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.leonetic.event.Event;

public class Render3DEvent extends Event {
    private final PoseStack matrix;
    private final float delta;

    public Render3DEvent(PoseStack matrix, float delta) {
        this.matrix = matrix;
        this.delta = delta;
    }

    public PoseStack getMatrix() {
        return matrix;
    }

    public float getDelta() {
        return delta;
    }
}
