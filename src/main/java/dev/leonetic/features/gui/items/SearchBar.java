package dev.leonetic.features.gui.items;

import dev.leonetic.features.gui.GuiTheme;
import dev.leonetic.features.gui.Widget;
import dev.leonetic.util.render.RenderUtil;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.Color;

public class SearchBar extends Item {
    public static String QUERY = "";

    private final TextBox textBox = new TextBox()
            .placeholder("Search...")
            .onChange(s -> QUERY = s);

    public SearchBar() {
        super("Search");
        this.height = GuiTheme.MODULE_HEIGHT;
    }

    public static boolean isFocused() { return TextBox.hasActiveFocus(); }

    public static boolean hasQuery() { return QUERY != null && !QUERY.isEmpty(); }

    public static boolean matches(String name) {
        if (!hasQuery()) return true;
        return name.toLowerCase().contains(QUERY.toLowerCase());
    }

    public static void unfocus() { TextBox.unfocusAll(); }

    @Override
    public int getHeight() { return GuiTheme.MODULE_HEIGHT; }

    @Override
    public void drawScreen(GuiGraphics context, int mouseX, int mouseY, float partialTicks) {
        boolean hovering = isHovering(mouseX, mouseY);
        Color accent = Widget.currentAccent != null ? Widget.currentAccent : new Color(70, 75, 82);

        float x1 = this.x;
        float y1 = this.y;
        float x2 = this.x + this.width;
        float y2 = this.y + this.height - 1f;

        textBox.renderPill(context, x1, y1, x2, y2, accent, hovering, 12f);

        float iconX = this.x + 4f;
        float iconY = this.y + (this.height - 6f) / 2f;
        int iconColor = textBox.isFocused() || hasQuery()
                ? GuiTheme.brighten(accent, 0.55f).getRGB()
                : GuiTheme.TEXT_SETTING_VALUE;
        drawMagnifier(context, iconX, iconY, iconColor);
    }

    private void drawMagnifier(GuiGraphics context, float x, float y, int color) {
        RenderUtil.rect(context, x, y, x + 5f, y + 1f, color);
        RenderUtil.rect(context, x, y + 4f, x + 5f, y + 5f, color);
        RenderUtil.rect(context, x, y, x + 1f, y + 5f, color);
        RenderUtil.rect(context, x + 4f, y, x + 5f, y + 5f, color);
        RenderUtil.rect(context, x + 5f, y + 5f, x + 7f, y + 6f, color);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton != 0) return;
        if (isHovering(mouseX, mouseY)) {
            textBox.focus();
        } else {
            textBox.unfocus();
        }
    }

    @Override
    public void keyActivate() {

        textBox.focus();
    }

    public void clear() {
        textBox.setText("");
        textBox.unfocus();
    }

    @Override
    public boolean isHovering(int mouseX, int mouseY) {
        return mouseX >= this.x && mouseX <= this.x + this.width
                && mouseY >= this.y && mouseY < this.y + this.height;
    }
}
