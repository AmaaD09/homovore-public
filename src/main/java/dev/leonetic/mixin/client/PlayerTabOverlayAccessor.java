package dev.leonetic.mixin.client;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(PlayerTabOverlay.class)
public interface PlayerTabOverlayAccessor {

    @Accessor("header")
    Component homovore$getHeader();

    @Accessor("footer")
    Component homovore$getFooter();
}
