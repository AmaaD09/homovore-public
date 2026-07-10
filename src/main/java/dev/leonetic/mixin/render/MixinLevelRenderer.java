package dev.leonetic.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.leonetic.Homovore;
import dev.leonetic.event.impl.render.RenderBlockOutlineEvent;
import dev.leonetic.features.modules.render.NoRenderModule;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.state.LevelRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static dev.leonetic.util.traits.Util.EVENT_BUS;

@Mixin(LevelRenderer.class)
public class MixinLevelRenderer {
    @Inject(method = "renderBlockOutline", at = @At("HEAD"), cancellable = true)
    public void renderBlockOutline(MultiBufferSource.BufferSource bufferSource, PoseStack poseStack, boolean bl, LevelRenderState levelRenderState, CallbackInfo ci) {
        if (EVENT_BUS.post(new RenderBlockOutlineEvent())) {
            ci.cancel();
        }
    }

    @Inject(method = "doesMobEffectBlockSky", at = @At("HEAD"), cancellable = true)
    private void homovore$noDarkness(Camera camera, CallbackInfoReturnable<Boolean> cir) {
        if (NoRenderModule.isActive(m -> m.noDarkness.getValue())) {
            cir.setReturnValue(false);
        }
    }

}
