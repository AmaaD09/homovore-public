package dev.leonetic.mixin.render.gui;

import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(Gui.class)
public abstract class MixinHeldItemTooltip {
    private static final int HOMOVORE$Y_OFFSET = 9;

    @ModifyArg(
        method = "renderSelectedItemName",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gui/GuiGraphics;drawStringWithBackdrop(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIII)V"
        ),
        index = 3
    )
    private int homovore$shiftHeldItemName(int y) {
        return y - HOMOVORE$Y_OFFSET;
    }
}
