package dev.leonetic.event.impl.entity;

import dev.leonetic.event.Event;
import net.minecraft.world.entity.LivingEntity;

public class TotemPopEvent extends Event {
    private final LivingEntity entity;

    public TotemPopEvent(LivingEntity entity) {
        this.entity = entity;
    }

    public LivingEntity getEntity() {
        return entity;
    }
}
