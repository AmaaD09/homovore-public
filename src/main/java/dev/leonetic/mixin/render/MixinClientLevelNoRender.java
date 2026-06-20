package dev.leonetic.mixin.render;

import dev.leonetic.features.modules.render.NoRenderModule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class MixinClientLevelNoRender {
    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void homovore$noDestroyBlockEffect(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (NoRenderModule.isActive(m -> m.noBlockBreak.getValue())) {
            ci.cancel();
        }
    }

    @Inject(method = "addBreakingBlockEffect", at = @At("HEAD"), cancellable = true)
    private void homovore$noBreakingBlockEffect(BlockPos pos, Direction direction, CallbackInfo ci) {
        if (NoRenderModule.isActive(m -> m.noBlockBreak.getValue())) {
            ci.cancel();
        }
    }

    @Inject(method = "trackExplosionEffects", at = @At("HEAD"), cancellable = true)
    private void homovore$noExplosionTracker(Vec3 center, float radius, int blockCount, WeightedList<ExplosionParticleInfo> blockParticles, CallbackInfo ci) {
        if (NoRenderModule.isActive(m -> m.noExplosion.getValue())) {
            ci.cancel();
        }
    }
}
