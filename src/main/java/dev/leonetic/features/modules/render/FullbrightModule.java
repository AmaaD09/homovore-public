package dev.leonetic.features.modules.render;

import dev.leonetic.features.modules.Module;
import dev.leonetic.mixin.client.OptionInstanceAccessor;

public class FullbrightModule extends Module {
    private double previousGamma;

    public FullbrightModule() {
        super("Fullbright", "Sets gamma to maximum for full brightness", Category.RENDER);
    }

    @Override
    public void onEnable() {
        previousGamma = mc.options.gamma().get();
        setGamma(16.0);
    }

    @Override
    public void onDisable() {
        setGamma(previousGamma);
    }

    @Override
    public void onTick() {
        setGamma(16.0);
    }

    @SuppressWarnings("unchecked")
    private void setGamma(double value) {
        ((OptionInstanceAccessor<Double>) (Object) mc.options.gamma()).homovore$setValue(value);
    }
}
