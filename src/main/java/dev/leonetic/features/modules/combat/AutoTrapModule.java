package dev.leonetic.features.modules.combat;

import dev.leonetic.Homovore;
import dev.leonetic.event.impl.entity.player.TickEvent;
import dev.leonetic.event.impl.render.Render3DEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.modules.Module;
import dev.leonetic.features.modules.client.TargetsModule;
import dev.leonetic.features.modules.world.SpeedMineModule;
import dev.leonetic.features.settings.Setting;
import dev.leonetic.util.PlaceUtil;
import dev.leonetic.util.inventory.InventoryUtil;
import dev.leonetic.util.inventory.ResultType;
import dev.leonetic.util.render.RenderUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AutoTrapModule extends Module {

    private final Setting<Double> range      = num("TargetRange", 8.0, 1.0, 16.0).setPage("General");
    private final Setting<Double> reach      = num("Reach", 5.5, 3.0, 6.0).setPage("General");
    private final Setting<Integer> predict   = num("LeadTicks", 2, 0, 6).setPage("General");
    private final Setting<Boolean> selfToggle = bool("SelfToggle", true).setPage("General");
    private final Setting<Boolean> debug      = bool("Debug", false).setPage("General");

    private final Setting<Boolean> render     = bool("Render", true).setPage("Render");
    private final Setting<Float> lineWidth    = num("LineWidth", 1.0f, 0.5f, 5.0f).setPage("Render");
    private final Setting<Color> fillColor    = color("FillColor", 255, 50, 50, 45).setPage("Render");
    private final Setting<Color> outlineColor = color("OutlineColor", 255, 50, 50, 160).setPage("Render");

    private final Set<BlockPos> wanted = new HashSet<>(16);
    private final Set<BlockPos> owned  = new HashSet<>(16);
    private final List<BlockPos> order   = new ArrayList<>(16);
    private final List<BlockPos> scratch = new ArrayList<>(16);

    private static final long RESEND_MS = 250L;
    private final Map<BlockPos, Long> sentAt = new HashMap<>();

    private static final double MAX_PREDICT_SPEED = 2.0;

    private java.util.UUID trackedId;
    private Vec3 prevPos;
    private Vec3 prevPrevPos;

    public AutoTrapModule() {
        super("AutoTrap", "Predicts a target's path and encases them with airplaced blocks.", Category.COMBAT);
    }

    @Override
    public void onDisable() {
        pruneAll();
    }

    @Subscribe(priority = 1)
    private void onTick(TickEvent event) {
        if (nullCheck() || mc.screen != null) return;

        int slot = resolveSlot();
        if (slot < 0) return;

        LivingEntity target = findTarget();
        if (target == null) {
            trackedId = null;
            pruneAll();
            if (selfToggle.getValue()) disable();
            return;
        }

        Vec3 velocity = trackVelocity(target);

        OffhandModule offhand = Homovore.moduleManager.getModuleByClass(OffhandModule.class);
        if (offhand != null && offhand.shouldDeferForEat()) return;

        var interp = target.getInterpolation();
        Vec3 anchor = interp != null && interp.hasActiveInterpolation() ? interp.position() : target.position();

        double padding = reach.getValue() - mc.player.blockInteractionRange();

        int lead = target.onGround() ? 0 : predict.getValue();
        Vec3 leadVel = velocity.length() > MAX_PREDICT_SPEED
                ? velocity.normalize().scale(MAX_PREDICT_SPEED) : velocity;
        Vec3 predFeet = anchor.add(leadVel.scale(lead));

        boolean complete = buildCage(predFeet, target, padding);
        int cageCells = scratch.size();
        int reachable = wanted.size();
        boolean commit = complete || target.onGround();
        if (!commit) {
            wanted.clear();
            order.clear();
        }

        Homovore.placementManager.removeQueuedFor(p -> owned.contains(p) && !wanted.contains(p));
        owned.retainAll(wanted);
        sentAt.keySet().retainAll(wanted);

        int sentCount = 0;
        if (commit) {
            long now = System.currentTimeMillis();
            for (BlockPos pos : order) {
                Long last = sentAt.get(pos);
                if (last != null && now - last < RESEND_MS) continue;
                if (Homovore.placementManager.enqueue(pos, slot)) {
                    owned.add(pos);
                    sentAt.put(pos, now);
                    sentCount++;
                }
            }
            if (sentCount > 0) Homovore.placementManager.flushQueue();
        }

        if (debug.getValue()) {
            Homovore.LOGGER.info(
                    "[AutoTrap] pose={} grounded={} dist={} vel={} lag={} lead={} pred=[{}] tgt=[{}] cage={} reach={} complete={} sent={} queued={}",
                    target.getBbHeight() <= 1.0 ? "compact" : "upright",
                    target.onGround(),
                    String.format("%.2f", Math.sqrt(mc.player.distanceToSqr(target))),
                    String.format("%.3f", velocity.length()),
                    String.format("%.2f", anchor.distanceTo(target.position())),
                    lead,
                    fmt(predFeet),
                    fmt(target.position()),
                    cageCells,
                    reachable,
                    complete,
                    sentCount,
                    owned.size());
        }
    }

    private static String fmt(Vec3 v) {
        return String.format("%.1f,%.1f,%.1f", v.x, v.y, v.z);
    }

    private boolean buildCage(Vec3 feet, LivingEntity t, double padding) {
        scratch.clear();
        if (t.getBbHeight() <= 1.0) compactCells(feet, t, scratch);
        else uprightCells(feet, t, scratch);

        wanted.clear();
        order.clear();
        boolean complete = true;
        for (BlockPos pos : scratch) {
            if (speedMineClaims(pos)) continue;
            if (!mc.level.getBlockState(pos).canBeReplaced()) continue;
            if (mc.player.isWithinBlockInteractionRange(pos, padding) && PlaceUtil.canPlace(pos)) {
                if (wanted.add(pos)) order.add(pos);
            } else {
                complete = false;
            }
        }
        return complete && !order.isEmpty();
    }

    private boolean speedMineClaims(BlockPos pos) {
        SpeedMineModule mine = Homovore.moduleManager.getModuleByClass(SpeedMineModule.class);
        return mine != null && mine.isEnabled() && mine.alreadyBreaking(pos);
    }

    private void compactCells(Vec3 feet, LivingEntity t, List<BlockPos> out) {
        double w = t.getBbWidth() * 0.5;
        int y = Mth.floor(feet.y + t.getBbHeight() * 0.5);
        int minX = Mth.floor(feet.x - w), maxX = Mth.floor(feet.x + w);
        int minZ = Mth.floor(feet.z - w), maxZ = Mth.floor(feet.z + w);
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++) {
                out.add(new BlockPos(x, y - 1, z));
                out.add(new BlockPos(x, y + 1, z));
            }
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++)
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    int nx = x + d.getStepX(), nz = z + d.getStepZ();
                    if (nx >= minX && nx <= maxX && nz >= minZ && nz <= maxZ) continue;
                    out.add(new BlockPos(nx, y, nz));
                }
    }

    private void uprightCells(Vec3 feet, LivingEntity t, List<BlockPos> out) {
        double w = t.getBbWidth() * 0.5;
        int feetY = Mth.floor(feet.y);
        int headY = feetY + 1;
        int minX = Mth.floor(feet.x - w), maxX = Mth.floor(feet.x + w);
        int minZ = Mth.floor(feet.z - w), maxZ = Mth.floor(feet.z + w);

        if (!t.onGround()) {
            for (int x = minX; x <= maxX; x++)
                for (int z = minZ; z <= maxZ; z++)
                    out.add(new BlockPos(x, feetY - 1, z));
        }
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++)
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    int nx = x + d.getStepX(), nz = z + d.getStepZ();
                    if (nx >= minX && nx <= maxX && nz >= minZ && nz <= maxZ) continue;
                    out.add(new BlockPos(nx, headY, nz));
                }
        for (int x = minX; x <= maxX; x++)
            for (int z = minZ; z <= maxZ; z++)
                out.add(new BlockPos(x, feetY + 2, z));
    }

    private void pruneAll() {
        if (!owned.isEmpty()) {
            Homovore.placementManager.removeQueuedFor(owned::contains);
            owned.clear();
        }
        wanted.clear();
        order.clear();
        sentAt.clear();
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (!render.getValue() || wanted.isEmpty()) return;
        Color fc = fillColor.getValue();
        Color oc = outlineColor.getValue();
        float lw = lineWidth.getValue();
        for (BlockPos pos : wanted) {
            RenderUtil.drawBoxFilled(event.getMatrix(), pos, fc);
            RenderUtil.drawBox(event.getMatrix(), pos, oc, lw);
        }
    }

    private Vec3 trackVelocity(LivingEntity t) {
        Vec3 cur = t.position();
        if (!t.getUUID().equals(trackedId)) {
            trackedId = t.getUUID();
            prevPos = cur;
            prevPrevPos = cur;
        }
        Vec3 vel = cur.subtract(prevPrevPos).scale(0.5);
        prevPrevPos = prevPos;
        prevPos = cur;
        return vel;
    }

    private LivingEntity findTarget() {
        TargetsModule targets = Homovore.moduleManager.getModuleByClass(TargetsModule.class);
        double maxSq = range.getValue() * range.getValue();
        LivingEntity best = null;
        double bestSq = Double.MAX_VALUE;
        for (Player p : mc.level.players()) {
            if (p == mc.player) continue;
            if (targets != null && !targets.isValidPlayerTarget(p)) continue;
            double dSq = mc.player.distanceToSqr(p);
            if (dSq > maxSq || dSq >= bestSq) continue;
            bestSq = dSq;
            best = p;
        }
        return best;
    }

    private int resolveSlot() {
        var r = InventoryUtil.find(Items.OBSIDIAN, InventoryUtil.PLACE_SCOPE);
        return (r.found() && r.type() != ResultType.OFFHAND) ? r.slot() : -1;
    }
}
