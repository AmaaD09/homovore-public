package dev.leonetic.features.gui.items.buttons;

import dev.leonetic.features.gui.GuiTheme;
import dev.leonetic.features.gui.HomovoreGui;
import dev.leonetic.features.gui.Widget;
import dev.leonetic.features.gui.items.Item;
import dev.leonetic.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

import java.awt.Color;

public class Button extends Item {
    private boolean state;

    public Button(String name) {
        super(name);
        this.height = GuiTheme.MODULE_HEIGHT;
    }

    @Override
    public void drawScreen(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
        boolean hovering = this.isHovering(mouseX, mouseY);
        drawRow(context, this.x, this.y, this.x + this.width, this.y + this.height,
                this.getState(), hovering);
        int textColor = this.getState() ? GuiTheme.TEXT_MODULE_ON : GuiTheme.TEXT_MODULE_OFF;
        drawScrollableString(this.getName(), this.x + 4f,
                this.y + (this.height - 8) / 2f,
                textColor, this.width - 8, hovering);
    }

    public static void drawSelectionRing(GuiGraphics context, float x1, float y1, float x2, float y2,
                                         Color accent) {
        if (accent == null) accent = new Color(110, 120, 130);
        int inner = GuiTheme.withAlpha(GuiTheme.brighten(accent, 0.65f), 230).getRGB();
        int outer = GuiTheme.withAlpha(GuiTheme.brighten(accent, 0.45f), 140).getRGB();
        RenderUtil.rect(context, x1, y1, x2, y2, outer, 2f);
        RenderUtil.rect(context, x1, y1, x2, y2, inner, 1f);
        RenderUtil.rect(context, x1, y1, x2, y1 + 1f, GuiTheme.HIGHLIGHT_TOP);
    }

    @Override
    public void keyActivate() {

        onMouseClick();
    }

    public static void drawRow(GuiGraphics context, float x1, float y1, float x2, float y2,
                               boolean enabled, boolean hovering) {
        Color accent = Widget.currentAccent != null ? Widget.currentAccent : new Color(70, 75, 82);

        float fillBottom = y2 - 1f;
        if (enabled) {
            RenderUtil.rect(context, x1, y1, x2, fillBottom, accent.getRGB());
            if (hovering) {
                RenderUtil.rect(context, x1, y1, x2, fillBottom, GuiTheme.HIGHLIGHT_TOP);
            }
            RenderUtil.rect(context, x1, y1, x2, y1 + 1f, GuiTheme.HIGHLIGHT_TOP);
        } else if (hovering) {
            RenderUtil.rect(context, x1, y1, x2, fillBottom,
                    GuiTheme.withAlpha(accent, 60).getRGB());
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && this.isHovering(mouseX, mouseY)) {
            this.onMouseClick();
        }
    }

    public void onMouseClick() {
        this.state = !this.state;
        this.toggle();
        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1f));
    }

    public void toggle() {}

    public boolean getState() { return this.state; }

    @Override
    public int getHeight() { return GuiTheme.MODULE_HEIGHT; }

    public boolean isHovering(int mouseX, int mouseY) {
        for (Widget widget : HomovoreGui.getClickGui().getComponents()) {
            if (widget.drag) return false;
        }
        return mouseX >= this.getX() && mouseX <= this.getX() + this.getWidth()
                && mouseY >= this.getY() && mouseY < this.getY() + this.height;
    }
}
