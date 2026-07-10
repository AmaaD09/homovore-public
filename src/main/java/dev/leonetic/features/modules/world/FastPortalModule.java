package dev.leonetic.features.modules.world;

import dev.leonetic.Homovore;
import dev.leonetic.event.impl.entity.player.PreTickEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.modules.Module;
import dev.leonetic.features.settings.Setting;
import dev.leonetic.manager.RotationRequest;
import dev.leonetic.manager.SwapRequest;
import dev.leonetic.mixin.client.ClientLevelAccessor;
import dev.leonetic.util.inventory.InventoryUtil;
import dev.leonetic.util.inventory.Result;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static dev.leonetic.util.inventory.InventoryUtil.FULL_SCOPE;

public class FastPortalModule extends Module {
    public Setting<Float> cooldown = num("Cooldown", 2.0f, 0.5f, 20.0f);

    private int cooldownTicks = 0;

    public FastPortalModule() {
        super("FastPortal", "Throws an ender pearl when inside nether portals", Category.WORLD);
    }

    @Override
    public void onDisable() {
        cooldownTicks = 0;
    }

    @Subscribe
    private void onPreTick(PreTickEvent event) {
        if (nullCheck()) return;

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        Vec3 eye = mc.player.getEyePosition();
        BlockPos eyeBlockPos = BlockPos.containing(eye);

        if (!mc.level.getBlockState(eyeBlockPos).is(Blocks.NETHER_PORTAL)) {
            return;
        }

        Result pearl = InventoryUtil.find(Items.ENDER_PEARL, FULL_SCOPE);
        if (!pearl.found()) return;

        if (mc.player.getCooldowns().isOnCooldown(new ItemStack(Items.ENDER_PEARL))) return;

        Vec3 portalCenter = new Vec3(eyeBlockPos.getX(), eyeBlockPos.getY() - 1, eyeBlockPos.getZ());
        float yaw = calcYaw(eye, portalCenter);
        float pitch = calcPitch(eye, portalCenter);

        Homovore.rotationManager.submit(new RotationRequest("fastportal", 20, yaw, pitch, RotationRequest.Mode.SILENT));

        mc.gameMode.ensureHasSentCarriedItem();
        boolean sent = Homovore.swapManager.submit(new SwapRequest("FastPortal", 20, pearl, r -> {
            try (var handler = ((ClientLevelAccessor) mc.level).homovore$getBlockStatePredictionHandler().startPredicting()) {
                mc.getConnection().send(new ServerboundUseItemPacket(r.hand(), handler.currentSequence(), yaw, pitch));
            }
        }));

        if (sent) cooldownTicks = (int) (cooldown.getValue() * 20);
    }

    private float calcYaw(Vec3 eye, Vec3 target) {
        Vec3 diff = target.subtract(eye);
        return (float) Math.toDegrees(Math.atan2(-diff.x, diff.z));
    }

    private float calcPitch(Vec3 eye, Vec3 target) {
        Vec3 diff = target.subtract(eye);
        double horizontalDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        return (float) -Math.toDegrees(Math.atan2(diff.y, horizontalDist));
    }
}
