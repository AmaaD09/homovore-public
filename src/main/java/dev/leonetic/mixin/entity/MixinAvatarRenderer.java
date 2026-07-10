package dev.leonetic.mixin.entity;

import dev.leonetic.Homovore;
import dev.leonetic.features.modules.render.NametagsModule;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
public class MixinAvatarRenderer {
    @Inject(method = "submitNameTag", at = @At("HEAD"), cancellable = true)
    private void cancelVanillaNameTag(CallbackInfo ci) {
        NametagsModule nametags = Homovore.moduleManager.getModuleByClass(NametagsModule.class);
        if (nametags != null && nametags.isEnabled()) {
            ci.cancel();
        }
    }
}
