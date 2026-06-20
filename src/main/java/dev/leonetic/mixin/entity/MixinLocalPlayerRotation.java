package dev.leonetic.mixin.entity;

import dev.leonetic.Homovore;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.leonetic.util.traits.Util.mc;

@Mixin(value = LocalPlayer.class, priority = Integer.MAX_VALUE)
public class MixinLocalPlayerRotation {
    @Shadow private float xRotLast;
    @Unique private float origSilentXRot;
    @Unique private float origMotionYRot, origMotionXRot;

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void onSendPositionMotionHead(CallbackInfo ci) {
        if (Homovore.rotationManager == null || !Homovore.rotationManager.isRotating()) return;

        origMotionYRot = mc.player.getYRot();
        origMotionXRot = mc.player.getXRot();

        float newYaw = Homovore.rotationManager.getRotationYaw();
        float newPitch = Homovore.rotationManager.getRotationPitch();
        mc.player.setYRot(newYaw);
        mc.player.setXRot(newPitch);

        float deltaYaw = newYaw - Homovore.rotationManager.getServerYaw();
        Homovore.rotationManager.setServerDeltaYaw(deltaYaw);
        Homovore.rotationManager.setServerYaw(newYaw);
        Homovore.rotationManager.setServerPitch(newPitch);
    }

    @Inject(method = "sendPosition", at = @At("TAIL"))
    private void onSendPositionMotionTail(CallbackInfo ci) {

        if (Homovore.rotationManager == null || !Homovore.rotationManager.isRotating()) return;

        Homovore.rotationManager.setServerYaw(mc.player.getYRot());
        Homovore.rotationManager.setServerPitch(mc.player.getXRot());

        mc.player.setYRot(origMotionYRot);
        mc.player.setXRot(origMotionXRot);
    }

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void onSendPositionHead(CallbackInfo ci) {
        if (Homovore.rotationManager == null || !Homovore.rotationManager.isSilentSyncRequired()) return;

        origSilentXRot = mc.player.getXRot();
        xRotLast -= 4;
        float f = (float) ((Math.random() * 2.0 - 1.0) * 0.001f);
        mc.player.setXRot(Mth.clamp(origSilentXRot + f, -90.0f, 90.0f));
    }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void onSendPositionReturn(CallbackInfo ci) {
        if (Homovore.rotationManager == null || !Homovore.rotationManager.isSilentSyncRequired()) return;

        mc.player.setXRot(origSilentXRot);
        Homovore.rotationManager.setSilentSyncRequired(false);
    }
}
