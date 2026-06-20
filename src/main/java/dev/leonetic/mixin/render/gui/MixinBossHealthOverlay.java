package dev.leonetic.mixin.render.gui;

import dev.leonetic.features.modules.render.NoRenderModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossHealthOverlay.class)
public class MixinBossHealthOverlay {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void homovore$noBossBar(GuiGraphics context, CallbackInfo ci) {
        if (NoRenderModule.isActive(m -> m.noBossBar.getValue())) {
            ci.cancel();
        }
    }
}
