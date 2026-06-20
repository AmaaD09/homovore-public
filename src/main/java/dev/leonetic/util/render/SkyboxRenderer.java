package dev.leonetic.util.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.leonetic.features.modules.render.SkyboxModule;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.EnumMap;
import java.util.Map;

public final class SkyboxRenderer {
    private static final Identifier VERTEX_SHADER = Identifier.fromNamespaceAndPath("homovore", "core/skybox");

    private static final Map<SkyboxModule.Mode, RenderType> TYPES = new EnumMap<>(SkyboxModule.Mode.class);

    static {
        for (SkyboxModule.Mode mode : SkyboxModule.Mode.values()) {
            TYPES.put(mode, buildType(mode));
        }
    }

    private SkyboxRenderer() {
    }

    private static RenderType buildType(SkyboxModule.Mode mode) {
        RenderPipeline pipeline = RenderPipeline.builder()
                .withLocation("pipeline/skybox_" + mode.name().toLowerCase())
                .withVertexShader(VERTEX_SHADER)
                .withFragmentShader(Identifier.fromNamespaceAndPath("homovore", "core/skybox_" + mode.name().toLowerCase()))

                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withUniform("Globals", UniformType.UNIFORM_BUFFER)

                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)

                .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                .withDepthWrite(false)
                .withCull(false)
                .build();
        return RenderType.create("homovore_skybox_" + mode.name().toLowerCase(),
                RenderSetup.builder(pipeline).createRenderSetup());
    }

    public static void render(SkyboxModule.Mode mode, float r, float g, float b) {
        RenderType type = TYPES.get(mode);
        if (type == null) return;

        BufferBuilder buffer = Tesselator.getInstance()
                .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        buffer.addVertex(-1.0f, -1.0f, 0.0f).setColor(r, g, b, 1.0f);
        buffer.addVertex( 1.0f, -1.0f, 0.0f).setColor(r, g, b, 1.0f);
        buffer.addVertex( 1.0f,  1.0f, 0.0f).setColor(r, g, b, 1.0f);
        buffer.addVertex(-1.0f,  1.0f, 0.0f).setColor(r, g, b, 1.0f);
        type.draw(buffer.buildOrThrow());
    }
}
