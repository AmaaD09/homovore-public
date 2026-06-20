package dev.leonetic.mixin.entity;

import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(LocalPlayer.class)
public interface EntityRotationAccessor {
    @Accessor("xBob")
    float homovore$getXBob();

    @Accessor("xBob")
    void homovore$setXBob(float value);

    @Accessor("xBobO")
    float homovore$getXBobO();

    @Accessor("xBobO")
    void homovore$setXBobO(float value);

    @Accessor("yBob")
    float homovore$getYBob();

    @Accessor("yBob")
    void homovore$setYBob(float value);

    @Accessor("yBobO")
    float homovore$getYBobO();

    @Accessor("yBobO")
    void homovore$setYBobO(float value);

    @Accessor("lastOnGround")
    boolean homovore$getLastOnGround();
}
