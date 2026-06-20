package dev.leonetic.event.impl.render;

import dev.leonetic.event.Event;
import net.minecraft.client.gui.GuiGraphics;

public class Render2DEvent extends Event {
    private final GuiGraphics context;
    private final float delta;

    public Render2DEvent(GuiGraphics context, float delta) {
        this.context = context;
        this.delta = delta;
    }

    public GuiGraphics getContext() {
        return context;
    }

    public float getDelta() {
        return delta;
    }
}
