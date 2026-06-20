package dev.leonetic.manager;

import java.util.function.Supplier;

public class RotationRequest {
    public final String id;
    public final int priority;
    public final Mode mode;

    public final boolean sticky;

    public final boolean noJitter;

    private final boolean dynamic;
    private final Supplier<Float> yawSupplier;
    private final Supplier<Float> pitchSupplier;

    public float targetYaw;
    public float targetPitch;

    public enum Mode {

        MOTION,

        SILENT
    }

    public RotationRequest(String id, int priority, float yaw, float pitch, Mode mode) {
        this(id, priority, yaw, pitch, mode, false);
    }

    public RotationRequest(String id, int priority, float yaw, float pitch, Mode mode, boolean sticky) {
        this(id, priority, yaw, pitch, mode, sticky, false);
    }

    public RotationRequest(String id, int priority, float yaw, float pitch, Mode mode, boolean sticky, boolean noJitter) {
        this.id = id;
        this.priority = priority;
        this.mode = mode;
        this.sticky = sticky;
        this.noJitter = noJitter;
        this.dynamic = false;
        this.targetYaw = yaw;
        this.targetPitch = pitch;
        this.yawSupplier = null;
        this.pitchSupplier = null;
    }

    public RotationRequest(String id, int priority, float yaw, float pitch) {
        this(id, priority, yaw, pitch, Mode.MOTION);
    }

    public RotationRequest(String id, int priority, Supplier<Float> yawSupplier, Supplier<Float> pitchSupplier, Mode mode) {
        this.id = id;
        this.priority = priority;
        this.mode = mode;
        this.sticky = false;
        this.noJitter = false;
        this.dynamic = true;
        this.yawSupplier = yawSupplier;
        this.pitchSupplier = pitchSupplier;
        updateTarget();
    }

    public boolean shouldUpdate() {
        return dynamic;
    }

    public void updateTarget() {
        if (dynamic && yawSupplier != null && pitchSupplier != null) {
            targetYaw = yawSupplier.get();
            targetPitch = pitchSupplier.get();
        }
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RotationRequest other && this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
