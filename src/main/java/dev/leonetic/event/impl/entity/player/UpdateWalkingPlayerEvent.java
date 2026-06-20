package dev.leonetic.event.impl.entity.player;

import dev.leonetic.event.Event;
import dev.leonetic.event.Stage;

public class UpdateWalkingPlayerEvent extends Event {
    private final Stage stage;

    public UpdateWalkingPlayerEvent(Stage stage) {
        this.stage = stage;
    }

    public Stage getStage() {
        return stage;
    }
}
