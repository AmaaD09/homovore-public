package dev.leonetic.util.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import dev.leonetic.mixin.render.RenderSetupAccessor;
import dev.leonetic.mixin.render.RenderTypeAccessor;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class SeeThroughRender {

    public static final int SEED_ORDER_BASE = 100_000;

    public static final int COLOR_ORDER_BASE = 200_000;

    public static boolean active;

    public static boolean cloning;

    public static int cloneSubOrder;

    private static final ConcurrentHashMap<RenderType, RenderType> SEED_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<RenderType, RenderType> COLOR_CACHE = new ConcurrentHashMap<>();

    private SeeThroughRender() {}

    public static void begin() {
        active = true;
        cloneSubOrder = 0;
    }

    public static void end() {
        active = false;
    }

    public static boolean isGlintType(RenderType type) {
        if (type == null) return false;
        String name = type.toString().toLowerCase(Locale.ROOT);
        return name.contains("glint") || name.contains("foil");
    }

    public static RenderType wrapDepthSeed(RenderType original) {
        if (original == null) return null;
        return SEED_CACHE.computeIfAbsent(original, o ->
                build(o, "_homovore_seethrough_seed",
                        DepthTestFunction.GREATER_DEPTH_TEST, true, false, Boolean.FALSE));
    }

    public static RenderType wrapColor(RenderType original) {
        if (original == null) return null;
        return COLOR_CACHE.computeIfAbsent(original, o ->
                build(o, "_homovore_seethrough_color",
                        DepthTestFunction.LEQUAL_DEPTH_TEST, true, true, null));
    }

    private static RenderType build(RenderType original, String suffix, DepthTestFunction depthFunc,
                                    boolean depthWrite, boolean colorWrite, Boolean cullOverride) {
        try {
            RenderSetup origSetup = ((RenderTypeAccessor) (Object) original).homovore$getState();
            RenderSetupAccessor sa = (RenderSetupAccessor) (Object) origSetup;

            RenderPipeline pipeline = clonePipeline(original.pipeline(), depthFunc, depthWrite, colorWrite, cullOverride);

            RenderSetup.RenderSetupBuilder b = RenderSetup.builder(pipeline);
            for (Map.Entry<String, RenderSetup.TextureBinding> e : sa.homovore$getTextures().entrySet()) {
                RenderSetup.TextureBinding binding = e.getValue();
                if (binding.sampler() != null) {
                    b.withTexture(e.getKey(), binding.location(), binding.sampler());
                } else {
                    b.withTexture(e.getKey(), binding.location());
                }
            }
            if (sa.homovore$useLightmap()) b.useLightmap();
            if (sa.homovore$useOverlay()) b.useOverlay();
            if (sa.homovore$affectsCrumbling()) b.affectsCrumbling();
            if (sa.homovore$sortOnUpload()) b.sortOnUpload();
            b.bufferSize(sa.homovore$getBufferSize());
            b.setLayeringTransform(sa.homovore$getLayeringTransform());
            b.setTextureTransform(sa.homovore$getTextureTransform());
            b.setOutline(sa.homovore$getOutlineProperty());

            return RenderType.create(original.toString() + suffix, b.createRenderSetup());
        } catch (Throwable t) {
            return original;
        }
    }

    private static RenderPipeline clonePipeline(RenderPipeline orig, DepthTestFunction depthFunc,
                                                boolean depthWrite, boolean colorWrite, Boolean cullOverride) {
        RenderPipeline.Builder b = RenderPipeline.builder()
                .withLocation(orig.getLocation())
                .withVertexShader(orig.getVertexShader())
                .withFragmentShader(orig.getFragmentShader())
                .withDepthTestFunction(depthFunc)
                .withPolygonMode(orig.getPolygonMode())
                .withCull(cullOverride != null ? cullOverride : orig.isCull())
                .withColorWrite(colorWrite && orig.isWriteColor(), colorWrite && orig.isWriteAlpha())
                .withDepthWrite(depthWrite)
                .withColorLogic(orig.getColorLogic())
                .withVertexFormat(orig.getVertexFormat(), orig.getVertexFormatMode())
                .withDepthBias(orig.getDepthBiasScaleFactor(), orig.getDepthBiasConstant());

        Optional<BlendFunction> blend = orig.getBlendFunction();
        if (blend.isPresent()) b.withBlend(blend.get());
        else b.withoutBlend();

        for (String s : orig.getSamplers()) b.withSampler(s);
        for (RenderPipeline.UniformDescription u : orig.getUniforms()) {
            if (u.textureFormat != null) {
                b.withUniform(u.name, u.type, u.textureFormat);
            } else {
                b.withUniform(u.name, u.type);
            }
        }

        ShaderDefines defs = orig.getShaderDefines();
        for (Map.Entry<String, String> e : defs.values().entrySet()) {
            String name = e.getKey();
            String value = e.getValue();
            try {
                b.withShaderDefine(name, Integer.parseInt(value));
            } catch (NumberFormatException nfe1) {
                try {
                    b.withShaderDefine(name, Float.parseFloat(value));
                } catch (NumberFormatException ignored) {

                }
            }
        }
        for (String flag : defs.flags()) b.withShaderDefine(flag);

        return b.build();
    }
}
