package dev.leonetic.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.leonetic.features.modules.render.NoRenderModule;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CustomHeadLayer.class)
public class MixinCustomHeadLayer<S extends LivingEntityRenderState, M extends EntityModel<S> & HeadedModel> {
    @Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;FF)V", at = @At("HEAD"), cancellable = true)
    private void homovore$noPlayerHead(PoseStack poseStack, SubmitNodeCollector collector, int packedLight, S renderState, float f, float g, CallbackInfo ci) {
        if (NoRenderModule.isActive(m -> m.noArmor.getValue()) && renderState.entityType == EntityType.PLAYER) {
            ci.cancel();
        }
    }
}
