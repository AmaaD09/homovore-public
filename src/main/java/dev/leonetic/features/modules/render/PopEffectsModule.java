package dev.leonetic.features.modules.render;

import dev.leonetic.Homovore;
import dev.leonetic.features.modules.Module;
import dev.leonetic.features.settings.Setting;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.ParticleTypes;

import java.awt.Color;

public class PopEffectsModule extends Module {

    public final Setting<Color> primaryColor =
            color("Primary", 0xE8, 0xFF, 0x55, 0xFF).setPage("Particles");
    public final Setting<Color> accentColor =
            color("Accent", 0x55, 0xFF, 0x55, 0xFF).setPage("Particles");
    public final Setting<Float> scale =
            num("Scale", 1.0f, 0.1f, 5.0f).setPage("Particles");

    public PopEffectsModule() {
        super("PopEffects", "Customize totem pop particle effects", Category.RENDER);
    }

    public static PopEffectsModule get() {
        if (Homovore.moduleManager == null) return null;
        return Homovore.moduleManager.getModuleByClass(PopEffectsModule.class);
    }

    public boolean shouldCustomize(ParticleType<?> type) {
        return isEnabled() && type == ParticleTypes.TOTEM_OF_UNDYING;
    }

    public int getRgb(boolean yellowVariant) {
        Color c = yellowVariant ? primaryColor.getValue() : accentColor.getValue();
        return (c.getRed() << 16) | (c.getGreen() << 8) | c.getBlue();
    }

    public float getScale() {
        return Math.max(0.1f, scale.getValue());
    }
}
