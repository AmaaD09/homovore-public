package dev.leonetic.manager;

import dev.leonetic.util.inventory.Result;

import java.util.function.Consumer;

public class SwapRequest {
    public final String id;
    public final int priority;
    public final Result target;
    public final Consumer<Result> action;

    public final boolean silent;

    public SwapRequest(String id, int priority, Result target, Runnable action) {
        this(id, priority, target, r -> action.run(), false);
    }

    public SwapRequest(String id, int priority, Result target, Consumer<Result> action) {
        this(id, priority, target, action, false);
    }

    public SwapRequest(String id, int priority, Result target, Runnable action, boolean silent) {
        this(id, priority, target, r -> action.run(), silent);
    }

    public SwapRequest(String id, int priority, Result target, Consumer<Result> action, boolean silent) {
        this.id = id;
        this.priority = priority;
        this.target = target;
        this.action = action;
        this.silent = silent;
    }
}
