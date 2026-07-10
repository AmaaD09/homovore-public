package dev.leonetic.features.modules.funny;

import dev.leonetic.Homovore;
import dev.leonetic.event.impl.entity.player.TickEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.modules.Module;

import dev.leonetic.manager.RotationRequest;
import dev.leonetic.manager.SwapRequest;
import dev.leonetic.mixin.client.ClientLevelAccessor;
import dev.leonetic.util.inventory.InventoryUtil;
import dev.leonetic.util.inventory.Result;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

public class TrickshotModule extends Module {

    private static final double   TARGET_X  = 0.5;
    private static final double   TARGET_Y  = 62.0;
    private static final double   TARGET_Z  = 0.5;
    private static final BlockPos TARGET_BP = BlockPos.containing(TARGET_X, TARGET_Y, TARGET_Z);
    private static final double   MAX_XZ_DIST = 60.0;

    private static final double PEARL_SPEED = 1.5;
    private static final double DRAG        = 0.99;
    private static final double GRAVITY     = 0.03;

    public TrickshotModule() {
        super("Trickshot", "Throws an ender pearl to 0 62 0 in the End.", Category.FUNNY);
    }

    @Subscribe
    private void onTick(TickEvent event) {
        if (mc.player == null || mc.level == null) { disable(); return; }
        if (!mc.level.dimension().identifier().getPath().equals("the_end")) { disable(); return; }

        double px = mc.player.getX(), pz = mc.player.getZ();
        if (px * px + pz * pz > MAX_XZ_DIST * MAX_XZ_DIST) { disable(); return; }

        Result pearl = InventoryUtil.find(Items.ENDER_PEARL, InventoryUtil.FULL_SCOPE);
        if (!pearl.found()) { disable(); return; }

        if (mc.player.getCooldowns().isOnCooldown(new ItemStack(Items.ENDER_PEARL))) { disable(); return; }

        Vec3 eye = mc.player.getEyePosition();

        Vec3 spawnPos = eye.subtract(0, 0.1, 0);

        double dX = TARGET_X - eye.x, dZ = TARGET_Z - eye.z;
        if (dX * dX + dZ * dZ < 0.001) { disable(); return; }

        float yaw = (float) Math.toDegrees(Math.atan2(-dX, dZ));

        Float pitchResult = calculatePitch(yaw, spawnPos);
        if (pitchResult == null) { disable(); return; }

        float pitch = pitchResult;

        Homovore.rotationManager.submit(new RotationRequest(
         "trickshot", 20, yaw, pitch, RotationRequest.Mode.SILENT
         ));

        mc.gameMode.ensureHasSentCarriedItem();
        Homovore.swapManager.submit(new SwapRequest("Trickshot", 20, pearl, r -> {
            try (var handler = ((ClientLevelAccessor) mc.level).homovore$getBlockStatePredictionHandler().startPredicting()) {
                mc.getConnection().send(new ServerboundUseItemPacket(r.hand(), handler.currentSequence(), yaw, pitch));
            }
        }));

        disable();
    }

    private Float calculatePitch(float yaw, Vec3 spawnPos) {
        Float fallback = null;

        for (float pitch = -80f; pitch <= 80f; pitch += 0.25f) {
            Double vy = tryPitch(yaw, pitch, spawnPos);
            if (vy == null) continue;
            if (vy < 0) return pitch;
            if (fallback == null) fallback = pitch;
        }

        return fallback;
    }

    private Double tryPitch(float yaw, float pitch, Vec3 spawnPos) {
        double yawRad   = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        double vx = -Math.sin(yawRad) * Math.cos(pitchRad) * PEARL_SPEED;
        double vy = -Math.sin(pitchRad) * PEARL_SPEED;
        double vz =  Math.cos(yawRad)  * Math.cos(pitchRad) * PEARL_SPEED;

        double x = spawnPos.x, y = spawnPos.y, z = spawnPos.z;

        for (int t = 0; t < 1000; t++) {

            vy -= GRAVITY;
            vx *= DRAG; vy *= DRAG; vz *= DRAG;
            x += vx; y += vy; z += vz;

            BlockPos bp = BlockPos.containing(x, y, z);

            if (bp.equals(TARGET_BP)) return vy;

            if (!mc.level.getBlockState(bp).getCollisionShape(mc.level, bp).isEmpty()) return null;
        }

        return null;
    }
}
