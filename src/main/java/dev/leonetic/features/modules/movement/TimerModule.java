package dev.leonetic.features.modules.movement;

import dev.leonetic.Homovore;
import dev.leonetic.event.impl.entity.player.TickEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.modules.Module;
import dev.leonetic.features.settings.Setting;

public class TimerModule extends Module {

    private final Setting<Double> speed = num("Speed", 1.0, 0.0, 3.0);
    private final Setting<Boolean> movementOnly = bool("MovementOnly", false);

    public TimerModule() {
        super("Timer", "Speeds up or slows down the game clock.", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        Homovore.TIMER = speed.getValue().floatValue();
        Homovore.TIMER_MOVEMENT_ONLY = movementOnly.getValue();
    }

    @Override
    public void onDisable() {
        Homovore.TIMER = 1f;
        Homovore.TIMER_MOVEMENT_ONLY = false;
    }

    @Subscribe
    private void onTick(TickEvent event) {
        Homovore.TIMER = speed.getValue().floatValue();
        Homovore.TIMER_MOVEMENT_ONLY = movementOnly.getValue();
    }

    @Override
    public String getDisplayInfo() {
        return String.format("%.1fx", speed.getValue());
    }
}
