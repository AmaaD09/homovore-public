package dev.leonetic.manager;

import dev.leonetic.features.modules.client.ClickGuiModule;
import dev.leonetic.util.ColorUtil;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ColorManager {
    private final Map<String, Supplier<Color>> colors = new HashMap<>();

    public void register(String name, Supplier<Color> supplier) {
        colors.put(name, supplier);
    }

    public Color get(String name) {
        Supplier<Color> s = colors.get(name);
        return s != null ? s.get() : Color.RED;
    }

    public int getAsInt(String name) {
        return ColorUtil.toRGBA(get(name));
    }

    public int getAsIntFullAlpha(String name) {
        Color c = get(name);
        return ColorUtil.toRGBA(new Color(c.getRed(), c.getGreen(), c.getBlue(), 255));
    }

    public int getWithAlpha(String name, float offset, int alpha) {
        if (ClickGuiModule.getInstance().theme.getValue() == ClickGuiModule.Theme.RAINBOW) {
            return ColorUtil.rainbow((int) (offset / 10f * ClickGuiModule.getInstance().rainbowHue.getValue())).getRGB();
        }
        Color c = get(name);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha).getRGB();
    }
}
