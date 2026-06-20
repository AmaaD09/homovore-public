package dev.leonetic.mixin.client;

import dev.leonetic.Homovore;
import dev.leonetic.features.modules.render.TablistModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.awt.Color;
import java.util.Comparator;
import java.util.List;

@Mixin(PlayerTabOverlay.class)
public abstract class MixinPlayerTabOverlay {

    @Inject(method = "getPlayerInfos", at = @At("HEAD"), cancellable = true)
    private void homovore$filterPlayers(CallbackInfoReturnable<List<PlayerInfo>> cir) {
        TablistModule module = Homovore.moduleManager.getModuleByClass(TablistModule.class);
        if (module == null || !module.isEnabled()) return;

        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        if (conn == null) return;

        List<PlayerInfo> all = conn.getListedOnlinePlayers().stream().toList();
        List<PlayerInfo> filtered = all.stream()
                .filter(module::shouldShow)
                .sorted(Comparator.comparing(p -> p.getProfile().name(), String.CASE_INSENSITIVE_ORDER))
                .limit(80L)
                .toList();
        cir.setReturnValue(filtered);
    }

    @Inject(method = "getNameForDisplay", at = @At("RETURN"), cancellable = true)
    private void homovore$colorName(PlayerInfo info, CallbackInfoReturnable<Component> cir) {
        TablistModule module = Homovore.moduleManager.getModuleByClass(TablistModule.class);
        if (module == null || !module.isEnabled()) return;

        String name = info.getProfile().name();
        if (name == null) return;

        Color color;
        if (Homovore.enemyManager.isEnemy(name)) {
            color = module.enemyColor.getValue();
        } else if (Homovore.friendManager.isFriend(name)) {
            color = module.friendColor.getValue();
        } else {
            return;
        }

        Component original = cir.getReturnValue();
        cir.setReturnValue(original.copy().withStyle(Style.EMPTY.withColor(color.getRGB() & 0xFFFFFF)));
    }
}
