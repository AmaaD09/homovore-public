package dev.leonetic.mixin.render.gui;

import dev.leonetic.features.modules.render.NoRenderModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public class MixinScreen {
    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void homovore$noBackground(GuiGraphics context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (Minecraft.getInstance().level != null && NoRenderModule.isActive(m -> m.noBackground.getValue())) {
            ci.cancel();
        }
    }

    @Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
    private void homovore$noTransparentBackground(GuiGraphics context, CallbackInfo ci) {
        if (Minecraft.getInstance().level != null && NoRenderModule.isActive(m -> m.noBackground.getValue())) {
            ci.cancel();
        }
    }
}
