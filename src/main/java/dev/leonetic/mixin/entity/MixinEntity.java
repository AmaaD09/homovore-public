package dev.leonetic.mixin.entity;

import dev.leonetic.Homovore;
import dev.leonetic.features.modules.movement.VelocityModule;
import dev.leonetic.features.modules.render.ShadersModule;
import dev.leonetic.manager.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinEntity {

    @Inject(method = "getLookAngle", at = @At("HEAD"), cancellable = true)
    private void homovore$spoofLookAngle(CallbackInfoReturnable<Vec3> cir) {
        Entity self = (Entity) (Object) this;
        if (self != Minecraft.getInstance().player) return;

        RotationManager rm = Homovore.rotationManager;
        if (rm == null || !rm.isMoveFixEnabled() || !rm.isRotating()) return;

        cir.setReturnValue(self.calculateViewVector(rm.getRotationPitch(), rm.getRotationYaw()));
    }
    @Inject(method = "isCurrentlyGlowing", at = @At("RETURN"), cancellable = true)
    private void onIsCurrentlyGlowing(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;

        ShadersModule shaders = Homovore.moduleManager.getModuleByClass(ShadersModule.class);
        if (shaders != null && shaders.isEnabled() && shaders.shouldShader(self)) {
            cir.setReturnValue(true);
            return;
        }

    }

    @Inject(method = "getTeamColor", at = @At("RETURN"), cancellable = true)
    private void onGetTeamColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;

        ShadersModule shaders = Homovore.moduleManager.getModuleByClass(ShadersModule.class);
        if (shaders != null && shaders.isEnabled() && shaders.shouldShader(self)) {
            cir.setReturnValue(shaders.getRgbFor(self));
            return;
        }

    }

    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void cancelEntityPush(Entity other, CallbackInfo ci) {
        VelocityModule velocity = Homovore.moduleManager.getModuleByClass(VelocityModule.class);
        if (velocity == null || !velocity.isEnabled() || !velocity.entityPush.getValue() || !velocity.phaseConditionMet()) return;
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (localPlayer == null) return;
        Object self = this;
        if (self == localPlayer || other == localPlayer) {
            ci.cancel();
        }
    }
}
