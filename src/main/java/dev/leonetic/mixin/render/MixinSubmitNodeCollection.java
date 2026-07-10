package dev.leonetic.mixin.render;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.leonetic.util.render.SeeThroughRender;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SubmitNodeCollection.class)
public class MixinSubmitNodeCollection {

    @Shadow @Final private SubmitNodeStorage submitNodeStorage;

    @Inject(method = "submitModel", at = @At("RETURN"))
    private <S> void homovore$cloneModelForSeeThrough(Model<? super S> model, S modelState, PoseStack poseStack,
                                                      RenderType renderType, int packedLight, int overlay,
                                                      int blockPosition, TextureAtlasSprite sprite, int flags,
                                                      ModelFeatureRenderer.CrumblingOverlay crumbling,
                                                      CallbackInfo ci) {
        if (!SeeThroughRender.active || SeeThroughRender.cloning) return;
        if (SeeThroughRender.isGlintType(renderType)) return;
        int sub = SeeThroughRender.cloneSubOrder++;
        SeeThroughRender.cloning = true;
        try {
            RenderType seed = SeeThroughRender.wrapDepthSeed(renderType);
            submitNodeStorage.order(SeeThroughRender.SEED_ORDER_BASE + sub).submitModel(
                    model, modelState, poseStack, seed, packedLight, overlay, blockPosition, sprite, flags, crumbling);
            RenderType color = SeeThroughRender.wrapColor(renderType);
            submitNodeStorage.order(SeeThroughRender.COLOR_ORDER_BASE + sub).submitModel(
                    model, modelState, poseStack, color, packedLight, overlay, blockPosition, sprite, flags, crumbling);
        } finally {
            SeeThroughRender.cloning = false;
        }
    }

    @Inject(method = "submitModelPart", at = @At("RETURN"))
    private void homovore$cloneModelPartForSeeThrough(ModelPart part, PoseStack poseStack, RenderType renderType,
                                                      int packedLight, int overlay, TextureAtlasSprite sprite,
                                                      boolean copyNormals, boolean fullBright, int flags,
                                                      ModelFeatureRenderer.CrumblingOverlay crumbling, int data,
                                                      CallbackInfo ci) {
        if (!SeeThroughRender.active || SeeThroughRender.cloning) return;
        if (SeeThroughRender.isGlintType(renderType)) return;
        int sub = SeeThroughRender.cloneSubOrder++;
        SeeThroughRender.cloning = true;
        try {
            RenderType seed = SeeThroughRender.wrapDepthSeed(renderType);
            submitNodeStorage.order(SeeThroughRender.SEED_ORDER_BASE + sub).submitModelPart(
                    part, poseStack, seed, packedLight, overlay, sprite, copyNormals, fullBright, flags, crumbling, data);
            RenderType color = SeeThroughRender.wrapColor(renderType);
            submitNodeStorage.order(SeeThroughRender.COLOR_ORDER_BASE + sub).submitModelPart(
                    part, poseStack, color, packedLight, overlay, sprite, copyNormals, fullBright, flags, crumbling, data);
        } finally {
            SeeThroughRender.cloning = false;
        }
    }

    @Inject(method = "submitItem", at = @At("RETURN"))
    private void homovore$cloneItemForSeeThrough(PoseStack poseStack, ItemDisplayContext context, int packedLight,
                                                 int overlay, int data, int[] tints, List<BakedQuad> quads,
                                                 RenderType renderType, ItemStackRenderState.FoilType foil,
                                                 CallbackInfo ci) {
        if (!SeeThroughRender.active || SeeThroughRender.cloning) return;
        if (SeeThroughRender.isGlintType(renderType)) return;
        int sub = SeeThroughRender.cloneSubOrder++;
        SeeThroughRender.cloning = true;
        try {

            RenderType seed = SeeThroughRender.wrapDepthSeed(renderType);
            submitNodeStorage.order(SeeThroughRender.SEED_ORDER_BASE + sub).submitItem(
                    poseStack, context, packedLight, overlay, data, tints, quads, seed,
                    ItemStackRenderState.FoilType.NONE);
            RenderType color = SeeThroughRender.wrapColor(renderType);
            submitNodeStorage.order(SeeThroughRender.COLOR_ORDER_BASE + sub).submitItem(
                    poseStack, context, packedLight, overlay, data, tints, quads, color,
                    ItemStackRenderState.FoilType.NONE);
        } finally {
            SeeThroughRender.cloning = false;
        }
    }
}
