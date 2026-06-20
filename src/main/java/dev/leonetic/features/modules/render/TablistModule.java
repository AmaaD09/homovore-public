package dev.leonetic.features.modules.render;

import dev.leonetic.Homovore;
import dev.leonetic.features.modules.Module;
import dev.leonetic.features.settings.Setting;
import net.minecraft.client.multiplayer.PlayerInfo;

import java.awt.Color;

public class TablistModule extends Module {

    public Setting<Boolean> friends = bool("Friends", true);
    public Setting<Boolean> enemies = bool("Enemies", true);
    public final Setting<Color> friendColor = color("Friend Color",   0, 255, 100, 255);
    public final Setting<Color> enemyColor  = color("Enemy Color",  255,   0,   0, 255);

    public TablistModule() {
        super("Tablist", "Hides players in the tablist who aren't you, a friend, or an enemy", Category.RENDER);
    }

    public boolean shouldShow(PlayerInfo info) {
        String name = info.getProfile().name();
        if (name == null) return false;
        if (mc.player != null && name.equalsIgnoreCase(mc.player.getGameProfile().name())) return true;
        if (friends.getValue() && Homovore.friendManager.isFriend(name)) return true;
        if (enemies.getValue() && Homovore.enemyManager.isEnemy(name)) return true;
        return false;
    }
}
