package dev.leonetic.features.modules.player;

import dev.leonetic.features.modules.Module;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;

public class FakePlayerModule extends Module {

    private RemotePlayer fakePlayer;

    public FakePlayerModule() {
        super("FakePlayer", "Spawns a fake player for testing combat modules.", Category.PLAYER);
    }

    @Override
    public void onEnable() {
        if (nullCheck()) return;
        fakePlayer = new RemotePlayer(mc.level, mc.player.getGameProfile());
        fakePlayer.copyPosition(mc.player);
        fakePlayer.setHealth(20f);
        mc.level.addEntity(fakePlayer);
    }

    @Override
    public void onDisable() {
        if (fakePlayer != null) {
            mc.level.removeEntity(fakePlayer.getId(), null);
            fakePlayer = null;
        }
    }
}
