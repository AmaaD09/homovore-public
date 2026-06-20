package dev.leonetic.mixin.render;

import net.minecraft.client.renderer.rendertype.LayeringTransform;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(RenderSetup.class)
public interface RenderSetupAccessor {
    @Accessor("textures")
    Map<String, RenderSetup.TextureBinding> homovore$getTextures();

    @Accessor("textureTransform")
    TextureTransform homovore$getTextureTransform();

    @Accessor("outputTarget")
    OutputTarget homovore$getOutputTarget();

    @Accessor("outlineProperty")
    RenderSetup.OutlineProperty homovore$getOutlineProperty();

    @Accessor("useLightmap")
    boolean homovore$useLightmap();

    @Accessor("useOverlay")
    boolean homovore$useOverlay();

    @Accessor("affectsCrumbling")
    boolean homovore$affectsCrumbling();

    @Accessor("sortOnUpload")
    boolean homovore$sortOnUpload();

    @Accessor("bufferSize")
    int homovore$getBufferSize();

    @Accessor("layeringTransform")
    LayeringTransform homovore$getLayeringTransform();
}
