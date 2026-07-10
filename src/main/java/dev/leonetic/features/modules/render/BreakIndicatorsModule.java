package dev.leonetic.features.modules.render;

import dev.leonetic.Homovore;
import dev.leonetic.event.impl.network.PacketEvent;
import dev.leonetic.event.impl.render.Render3DEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.modules.Module;
import dev.leonetic.features.settings.Setting;
import dev.leonetic.util.InteractionUtil;
import dev.leonetic.util.render.RenderUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.awt.Color;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class BreakIndicatorsModule extends Module {

    private final Setting<Boolean> useDoubleminePrediction = bool("UseDoubleminePrediction", false).setPage("General");
    private final Setting<Float> rebreakCompletionAmount = num("RebreakCompletionAmount", 0.7f, 0.0f, 1.5f).setPage("General");
    private final Setting<Float> completionAmount = num("FullCompletionAmount", 1.0f, 0.0f, 1.5f).setPage("General");
    private final Setting<Float> removeCompletionAmount = num("ForceRemoveCompletionAmount", 1.3f, 0.0f, 1.5f).setPage("General");
    private final Setting<Boolean> ignoreFriends = bool("IgnoreFriends", false).setPage("General");
    private final Setting<Boolean> holdRebreak = bool("HoldRebreak", true).setPage("General");

    private final Setting<Boolean> render = bool("DoRender", true).setPage("Render");
    private final Setting<Float> lineWidth = num("LineWidth", 1.5f, 0.5f, 5.0f).setPage("Render");
    private final Setting<Color> sideColor = color("SideColor", 255, 0, 80, 10).setPage("Render");
    private final Setting<Color> lineColor = color("LineColor", 255, 255, 255, 40).setPage("Render");

    private static final long BREAK_TIME_MS = 2500L;

    private final Map<BlockPos, BreakEntry> breaks = new ConcurrentHashMap<>();

    public BreakIndicatorsModule() {
        super("BreakIndicators", "Renders the progress of a block being broken.", Category.RENDER);
    }

    @Override
    public void onEnable() {
        breaks.clear();
    }

    @Override
    public void onDisable() {
        breaks.clear();
    }

    @Subscribe
    private void onPacket(PacketEvent.Receive event) {
        if (mc.level == null || mc.player == null) return;
        if (!(event.getPacket() instanceof ClientboundBlockDestructionPacket packet)) return;

        BlockPos pos = packet.getPos().immutable();
        BlockState state = mc.level.getBlockState(pos);
        if (!InteractionUtil.canBreak(pos, state)) return;

        Entity entity = mc.level.getEntity(packet.getId());
        int id = packet.getId();
        long now = System.currentTimeMillis();

        // A new mine from this player clears their held rebreak markers.
        if (holdRebreak.getValue()) {
            breaks.values().removeIf(b -> b.held && b.entityId == id && !b.pos.equals(pos));
        }

        BreakEntry existing = breaks.get(pos);
        if (existing != null) {
            // Re-mining a held block restarts its progress animation.
            if (existing.held) {
                existing.held = false;
                existing.startMs = now;
            }
            return;
        }

        if (useDoubleminePrediction.getValue() && entity instanceof Player) {
            List<BreakEntry> playerBreaks = breaks.values().stream()
                    .filter(b -> b.entity == entity && !b.pos.equals(pos))
                    .sorted(Comparator.comparingLong(b -> b.startMs))
                    .toList();

            if (playerBreaks.size() >= 2) {
                breaks.remove(playerBreaks.getLast().pos);
            }
        }

        breaks.put(pos, new BreakEntry(pos, id, entity, now));
    }

    public boolean isBlockBeingBroken(BlockPos pos) {
        return breaks.containsKey(pos);
    }

    public boolean isBlockBeingBrokenByFriend(BlockPos pos) {
        BreakEntry entry = breaks.get(pos);
        return entry != null && entry.entity instanceof Player player && Homovore.friendManager.isFriend(player);
    }

    public boolean isBlockBeingBrokenBySelf(BlockPos pos) {
        if (mc.player == null) return false;
        BreakEntry entry = breaks.get(pos);
        return entry != null && entry.entityId == mc.player.getId();
    }

    public double getBlockBreakProgress(BlockPos pos) {
        BreakEntry entry = breaks.get(pos);
        if (entry == null) return 0.0;
        return entry.held ? 1.0 : Math.clamp(entry.progress(System.currentTimeMillis()), 0.0, 1.0);
    }

    public Map<BlockPos, BreakInfo> getActiveBreaksSnapshot() {
        Map<BlockPos, BreakInfo> snapshot = new HashMap<>();
        for (BreakEntry entry : breaks.values()) {
            if (entry.held) continue;
            snapshot.put(entry.pos, new BreakInfo(entry.pos, entry.entity instanceof Player player ? player : null));
        }
        return snapshot;
    }

    @Subscribe
    private void onRender(Render3DEvent event) {
        if (nullCheck()) return;

        long now = System.currentTimeMillis();

        Iterator<Map.Entry<BlockPos, BreakEntry>> iterator = breaks.entrySet().iterator();
        while (iterator.hasNext()) {
            BreakEntry entry = iterator.next().getValue();
            // Held rebreak markers persist until a new mine from that player (cleared in onPacket).
            if (entry.held) continue;

            BlockState state = mc.level.getBlockState(entry.pos);
            boolean finished = state.isAir()
                    || entry.progress(now) > removeCompletionAmount.getValue()
                    || !InteractionUtil.canBreak(entry.pos, state);
            if (finished) {
                if (holdRebreak.getValue()) {
                    entry.held = true;
                } else {
                    iterator.remove();
                }
            }
        }

        if (useDoubleminePrediction.getValue()) {
            Map<Player, List<BreakEntry>> playerBreakingBlocks = breaks.values().stream()
                    .filter(entry -> entry.entity instanceof Player)
                    .sorted(Comparator.comparingLong(entry -> entry.startMs))
                    .collect(Collectors.groupingBy(entry -> (Player) entry.entity, Collectors.toList()));

            for (List<BreakEntry> list : playerBreakingBlocks.values()) {
                list.forEach(entry -> entry.isRebreak = false);
                if (list.size() >= 2) list.getLast().isRebreak = true;
            }
        }

        if (!render.getValue()) return;

        for (BreakEntry entry : breaks.values()) {
            if (ignoreFriends.getValue() && entry.entity instanceof Player player
                    && Homovore.friendManager.isFriend(player)) {
                continue;
            }

            entry.renderBlock(event, now);
        }
    }

    public record BreakInfo(BlockPos pos, Player player) {}

    private final class BreakEntry {
        private final BlockPos pos;
        private final int entityId;
        private final Entity entity;
        private long startMs;
        private boolean isRebreak;
        private boolean held;

        private BreakEntry(BlockPos pos, int entityId, Entity entity, long startMs) {
            this.pos = pos;
            this.entityId = entityId;
            this.entity = entity;
            this.startMs = startMs;
        }

        private void renderBlock(Render3DEvent event, long now) {
            BlockState state = mc.level.getBlockState(pos);
            VoxelShape shape = state.getShape(mc.level, pos);
            if (shape.isEmpty()) {
                RenderUtil.drawBoxFilled(event.getMatrix(), pos, sideColor.getValue());
                RenderUtil.drawBox(event.getMatrix(), pos, lineColor.getValue(), lineWidth.getValue());
                return;
            }

            AABB orig = shape.bounds();

            double completion = isRebreak ? rebreakCompletionAmount.getValue() : completionAmount.getValue();
            double scale = held ? 1.0
                    : (completion <= 0.0 ? 1.0 : Math.clamp(progress(now) / completion, 0.0, 1.0));

            double centerX = (orig.minX + orig.maxX) * 0.5;
            double centerY = (orig.minY + orig.maxY) * 0.5;
            double centerZ = (orig.minZ + orig.maxZ) * 0.5;
            double halfX = orig.getXsize() * scale * 0.5;
            double halfY = orig.getYsize() * scale * 0.5;
            double halfZ = orig.getZsize() * scale * 0.5;

            double x1 = pos.getX() + centerX - halfX;
            double y1 = pos.getY() + centerY - halfY;
            double z1 = pos.getZ() + centerZ - halfZ;
            double x2 = pos.getX() + centerX + halfX;
            double y2 = pos.getY() + centerY + halfY;
            double z2 = pos.getZ() + centerZ + halfZ;

            AABB renderBox = new AABB(x1, y1, z1, x2, y2, z2);
            RenderUtil.drawBoxFilled(event.getMatrix(), renderBox, sideColor.getValue());
            RenderUtil.drawBox(event.getMatrix(), renderBox, lineColor.getValue(), lineWidth.getValue());
        }

        private double progress(long now) {
            return (double) (now - startMs) / BREAK_TIME_MS;
        }
    }
}
