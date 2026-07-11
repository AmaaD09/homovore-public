package dev.leonetic.mixin.entity;

import dev.leonetic.Homovore;
import dev.leonetic.features.modules.combat.AutoCrystalModule;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class MixinClientLevelEntity {
    @Inject(method = "addEntity", at = @At("TAIL"))
    private void homovore$onEntityAdded(Entity entity, CallbackInfo ci) {
        if (!(entity instanceof EndCrystal crystal) || Homovore.moduleManager == null) return;
        if (((ClientLevel) (Object) this).getEntity(entity.getId()) != entity) return;
        AutoCrystalModule autoCrystal = Homovore.moduleManager.getModuleByClass(AutoCrystalModule.class);
        if (autoCrystal != null && autoCrystal.isEnabled()) autoCrystal.onCrystalAdded(crystal);
    }
}
