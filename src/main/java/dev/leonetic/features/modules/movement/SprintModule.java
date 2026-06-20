package dev.leonetic.features.modules.movement;

import dev.leonetic.event.impl.entity.player.TickEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.modules.Module;

public class SprintModule extends Module {

    public SprintModule() {
        super("Sprint", "Automatically sprints whenever you move.", Category.MOVEMENT);
    }

    @Subscribe
    private void onTick(TickEvent event) {
        if (nullCheck()) return;
        if (mc.player.input.getMoveVector().y > 0) {
            mc.player.setSprinting(true);
        }
    }
}
