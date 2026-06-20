package dev.leonetic.event.impl.input;

import dev.leonetic.event.Event;

public class KeyInputEvent extends Event {
    private final int key;
    private final int action;

    public KeyInputEvent(int key, int action) {
        this.key = key;
        this.action = action;
    }

    public int getKey() {
        return key;
    }

    public int getAction() {
        return action;
    }
}
