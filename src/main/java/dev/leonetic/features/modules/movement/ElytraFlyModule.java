package dev.leonetic.features.modules.movement;

import dev.leonetic.Homovore;
import dev.leonetic.event.impl.entity.player.PreTickEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.modules.Module;
import dev.leonetic.features.settings.Setting;
import dev.leonetic.manager.RotationRequest;
import dev.leonetic.manager.SwapRequest;
import dev.leonetic.util.inventory.InventoryUtil;
import dev.leonetic.util.inventory.Result;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Items;

import static dev.leonetic.util.inventory.InventoryUtil.FULL_SCOPE;

public class ElytraFlyModule extends Module {

    public static final String ID = "ElytraFly";
    public static final int PRIORITY = 50;

    private static final int ROCKET_SWAP_PRIORITY = 15;

    private final Setting<Boolean> lockPitch = bool("LockPitch", true);

    private final Setting<Double> pitch = num("Pitch", 0.0, -45.0, 45.0);

    private final Setting<Double> climbPitch = num("ClimbPitch", 45.0, 15.0, 89.0);

    private final Setting<Boolean> hover = bool("Hover", false);

    private final Setting<Double> hoverPitch = num("HoverPitch", -1.3, -45.0, 0.0);

    private final Setting<Boolean> autoRocket = bool("AutoRocket", false);

    private boolean steering;

    private boolean hovering;

    private int idlePhase;

    private int ticksSinceRocket = Integer.MAX_VALUE / 2;

    private int refireTicks;

    public ElytraFlyModule() {
        super("ElytraFly", "Steer your elytra flight with WASD; auto-fires fireworks for thrust.", Category.MOVEMENT);
        pitch.setVisibility(v -> lockPitch.getValue());
        hoverPitch.setVisibility(v -> hover.getValue());
    }

    public boolean isSteering() {
        return steering;
    }

    @Override
    public void onDisable() {
        stop();
    }

    @Subscribe
    private void onPreTick(PreTickEvent event) {
        if (nullCheck() || !mc.player.isFallFlying()) {
            stop();
            return;
        }

        hovering = false;

        if (autoRocket.getValue()) tryAutoRocket();

        boolean forward = mc.options.keyUp.isDown();
        boolean back = mc.options.keyDown.isDown();
        boolean left = mc.options.keyLeft.isDown();
        boolean right = mc.options.keyRight.isDown();
        boolean up = mc.options.keyJump.isDown();
        boolean down = mc.options.keyShift.isDown();

        float realYaw = mc.player.getYRot();

        float strafe = (right ? 1f : 0f) - (left ? 1f : 0f);
        float fwd = (forward ? 1f : 0f) - (back ? 1f : 0f);
        boolean hasHorizontal = strafe != 0f || fwd != 0f;
        boolean wantUp = up && !down;
        boolean wantDown = down && !up;

        if (hasHorizontal || wantUp || wantDown) idlePhase = 0;

        if (hasHorizontal) {
            float yaw = headingFromInput(realYaw, strafe, fwd);
            float targetPitch;
            if (wantUp) {
                targetPitch = -climbPitch.getValue().floatValue();
            } else if (wantDown) {
                targetPitch = climbPitch.getValue().floatValue();
            } else {
                targetPitch = lockPitch.getValue() ? pitch.getValue().floatValue() : mc.player.getXRot();
            }
            steerTo(yaw, targetPitch);
            return;
        }

        if (wantUp) {
            steerTo(realYaw, -90f);
            return;
        }
        if (wantDown) {
            steerTo(realYaw, 90f);
            return;
        }

        if (hover.getValue()) {

            boolean lookBack = (idlePhase++ & 1) == 1;
            steerTo(lookBack ? Mth.wrapDegrees(realYaw + 180f) : realYaw, hoverPitch.getValue().floatValue());
            hovering = true;
        } else {
            stop();
        }
    }

    private void tryAutoRocket() {
        ticksSinceRocket++;
        if (ticksSinceRocket < refireTicks) return;

        Result rocket = InventoryUtil.find(Items.FIREWORK_ROCKET, FULL_SCOPE);
        if (!rocket.found()) return;

        ticksSinceRocket = 0;
        refireTicks = InventoryUtil.fireworkRefireTicks(rocket.stack());
        Homovore.swapManager.submit(new SwapRequest(ID, ROCKET_SWAP_PRIORITY, rocket,
                () -> mc.gameMode.useItem(mc.player, rocket.hand())));
    }

    private float headingFromInput(float cameraYaw, float strafe, float fwd) {
        double yawRad = Math.toRadians(cameraYaw);

        double fx = -Math.sin(yawRad), fz = Math.cos(yawRad);
        double rx = -Math.sin(yawRad + Math.PI / 2.0), rz = Math.cos(yawRad + Math.PI / 2.0);
        double wx = fx * fwd + rx * strafe;
        double wz = fz * fwd + rz * strafe;

        return (float) Math.toDegrees(Math.atan2(wz, wx)) - 90f;
    }

    private void steerTo(float yaw, float pitch) {
        Homovore.rotationManager.submit(new RotationRequest(
                ID, PRIORITY, yaw, Mth.clamp(pitch, -90f, 90f), RotationRequest.Mode.MOTION, true, true));
        steering = true;
    }

    private void stop() {
        hovering = false;
        idlePhase = 0;
        ticksSinceRocket = Integer.MAX_VALUE / 2;
        if (steering) {
            Homovore.rotationManager.cancel(ID);
            steering = false;
        }
    }

    @Override
    public String getDisplayInfo() {
        if (hovering) return "Hover";
        return steering ? "Steering" : null;
    }
}
