package dev.leonetic.features.modules.movement;

import dev.leonetic.Homovore;
import dev.leonetic.event.impl.entity.player.PreTickEvent;
import dev.leonetic.event.impl.network.PacketEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.modules.Module;
import dev.leonetic.features.settings.Setting;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class FreezeFallModule extends Module {

    private final Setting<Integer> packetGap = num("PacketGap", 20, 1, 100);

    private double frozenX = Double.NaN;
    private double frozenY = Double.NaN;
    private double frozenZ = Double.NaN;
    private int ticksSincePacket = 0;

    public FreezeFallModule() {
        super("FreezeFall", "Freezes fall by suppressing movement packets like Grim-safe elytra hover.", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        Homovore.FREEZE_FALL = true;
        frozenX = frozenY = frozenZ = Double.NaN;
        ticksSincePacket = 0;
    }

    @Override
    public void onDisable() {
        Homovore.FREEZE_FALL = false;
        frozenX = frozenY = frozenZ = Double.NaN;
    }

    @Subscribe
    private void onPreTick(PreTickEvent event) {
        if (mc.player == null) return;
        if (Double.isNaN(frozenY) && !mc.player.onGround()) {
            frozenX = mc.player.getX();
            frozenY = mc.player.getY();
            frozenZ = mc.player.getZ();
        }
        if (mc.player.onGround()) {
            frozenX = frozenY = frozenZ = Double.NaN;
        }
        if (!Double.isNaN(frozenY)) {
            mc.player.setDeltaMovement(0, 0, 0);
            mc.player.setPos(frozenX, frozenY, frozenZ);
        }
    }

    @Subscribe
    private void onPacketSend(PacketEvent.Send event) {
        if (mc.player == null || Double.isNaN(frozenY)) return;
        if (!(event.getPacket() instanceof ServerboundMovePlayerPacket pkt)) return;
        if (!pkt.hasPosition()) return;

        ticksSincePacket++;
        event.cancel();

        if (ticksSincePacket >= packetGap.getValue()) {
            ticksSincePacket = 0;
            boolean onGround = mc.player.onGround();
            boolean hc = mc.player.horizontalCollision;
            mc.player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                    frozenX,
                    frozenY,
                    frozenZ,
                    pkt.getYRot(mc.player.getYRot()),
                    pkt.getXRot(mc.player.getXRot()),
                    onGround,
                    hc
            ));
        }
    }
}
