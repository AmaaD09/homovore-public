package dev.leonetic.mixin.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.leonetic.Homovore;
import dev.leonetic.manager.RotationManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static dev.leonetic.util.traits.Util.mc;

@Mixin(LivingEntity.class)
public class MixinLivingEntityTravel {
    @Unique private float homovore$origYaw, homovore$origPitch;
    @Unique private boolean homovore$applied;
    @Unique private double homovore$frozenY = Double.NaN;

    @Inject(method = "travel", at = @At("HEAD"))
    private void homovore$travelHead(Vec3 movementInput, CallbackInfo ci) {
        homovore$applied = false;

        LivingEntity self = (LivingEntity) (Object) this;

        if (Homovore.FREEZE_FALL && self == mc.player && !self.onGround()) {
            if (Double.isNaN(homovore$frozenY)) {
                homovore$frozenY = self.getY();
            }
        } else if (self == mc.player && self.onGround()) {
            homovore$frozenY = Double.NaN;
        }

        if (self != mc.player || !homovore$spoofing()) return;

        homovore$origYaw = self.getYRot();
        homovore$origPitch = self.getXRot();
        self.setYRot(Homovore.rotationManager.getRotationYaw());
        self.setXRot(Homovore.rotationManager.getRotationPitch());
        homovore$applied = true;
    }

    @Inject(method = "travel", at = @At("RETURN"))
    private void homovore$travelReturn(Vec3 movementInput, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        if (Homovore.TIMER_MOVEMENT_ONLY && self == mc.player && Homovore.TIMER != 1f) {
            float scale = Homovore.TIMER;
            Vec3 vel = self.getDeltaMovement();
            self.setDeltaMovement(new Vec3(vel.x * scale, vel.y * scale, vel.z * scale));
        }

        if (Homovore.FREEZE_FALL && self == mc.player && !Double.isNaN(homovore$frozenY)) {
            self.setPos(self.getX(), homovore$frozenY, self.getZ());
            Vec3 vel = self.getDeltaMovement();
            self.setDeltaMovement(vel.x, 0, vel.z);
        }

        if (!homovore$applied) return;
        homovore$applied = false;

        self.setYRot(homovore$origYaw);
        self.setXRot(homovore$origPitch);
    }

    @ModifyExpressionValue(
            method = "jumpFromGround",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getYRot()F"))
    private float homovore$jumpYaw(float original) {
        if ((Object) this != mc.player || !homovore$spoofing()) return original;
        return Homovore.rotationManager.getRotationYaw();
    }

    @Unique
    private boolean homovore$spoofing() {
        RotationManager rm = Homovore.rotationManager;
        return rm != null && rm.isMoveFixEnabled() && rm.isRotating();
    }

    @ModifyExpressionValue(
            method = "handleRelativeFrictionAndCalculateMovement",
            at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/LivingEntity;horizontalCollision:Z"))
    private boolean homovore$noScaffoldClimb(boolean original) {
        if ((Object) this != mc.player || !original) return original;
        return !((LivingEntity) (Object) this).getInBlockState().is(Blocks.SCAFFOLDING);
    }
}
