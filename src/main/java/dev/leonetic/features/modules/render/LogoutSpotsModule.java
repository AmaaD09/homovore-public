package dev.leonetic.features.modules.render;

import dev.leonetic.Homovore;
import dev.leonetic.event.impl.network.DisconnectEvent;
import dev.leonetic.event.impl.render.Render2DEvent;
import dev.leonetic.event.impl.render.Render3DEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.modules.Module;
import dev.leonetic.features.settings.Setting;
import dev.leonetic.util.render.MatrixCapture;
import dev.leonetic.util.render.WireframeEntityRenderer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.awt.*;
import java.util.*;
import java.util.List;

public class LogoutSpotsModule extends Module {

    public Setting<Color>   spotColor = color("SpotColor",  255, 100, 100, 255);
    public Setting<Color>   sideColor = color("SideColor",  255, 100, 100,  55);
    public Setting<Float>   lineWidth = num("LineWidth",    1.5f, 0.5f, 5.0f);
    public Setting<Boolean> showTime  = bool("ShowTime",    true);
    public Setting<Color>   nameColor = color("NameColor",  255, 100, 100, 255);

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private final Map<UUID, SpotData>   playerCache       = new HashMap<>();

    private final Map<UUID, LogoutSpot> loggedPlayers     = new HashMap<>();

    private final Map<UUID, Integer>    ticksOnPlayerList = new HashMap<>();

    private ResourceKey<Level> lastDimension = null;
    private ClientLevel lastLevel = null;

    private Set<UUID> previousTabList = new HashSet<>();

    private Set<UUID> previouslyVisible = new HashSet<>();

    public LogoutSpotsModule() {
        super("LogoutSpots", "Shows where players logged out with a wireframe avatar", Category.RENDER);
    }

    @Override
    public void onEnable() {
        resetState();
    }

    @Override
    public void onDisable() {
        resetState();
    }

    @Override
    public void onTick() {
        if (nullCheck() || mc.getConnection() == null) return;

        ResourceKey<Level> dim = mc.level.dimension();
        if (mc.level != lastLevel || (lastDimension != null && !dim.equals(lastDimension))) {
            playerCache.clear();
            ticksOnPlayerList.clear();
            previousTabList.clear();
            previouslyVisible.clear();
        }
        lastLevel = mc.level;
        lastDimension = dim;

        Set<UUID> currentlyVisible = new HashSet<>();
        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            UUID id = player.getUUID();
            playerCache.put(id, new SpotData(player));
            currentlyVisible.add(id);
        }

        Set<UUID> currentTabList = new HashSet<>();
        for (var info : mc.getConnection().getOnlinePlayers()) {
            currentTabList.add(info.getProfile().id());
        }

        for (UUID uuid : previousTabList) {
            if (!currentTabList.contains(uuid) && !loggedPlayers.containsKey(uuid)) {
                if (previouslyVisible.contains(uuid)) {
                    SpotData data = playerCache.get(uuid);
                    if (data != null) {
                        loggedPlayers.put(uuid, new LogoutSpot(data, dim));
                    }
                }
                ticksOnPlayerList.remove(uuid);
            }
        }

        loggedPlayers.entrySet().removeIf(entry -> {
            UUID uuid = entry.getKey();
            if (currentTabList.contains(uuid)) {
                int n = ticksOnPlayerList.getOrDefault(uuid, 0) + 1;
                ticksOnPlayerList.put(uuid, n);
                if (n > 1) {
                    ticksOnPlayerList.remove(uuid);
                    return true;
                }
            } else {
                ticksOnPlayerList.remove(uuid);
            }
            return false;
        });

        previousTabList = currentTabList;
        previouslyVisible = currentlyVisible;
    }

    @Override
    public String getDisplayInfo() {
        if (nullCheck()) return loggedPlayers.isEmpty() ? null : String.valueOf(loggedPlayers.size());
        ResourceKey<Level> dim = mc.level.dimension();
        long n = loggedPlayers.values().stream().filter(s -> dim.equals(s.dimension)).count();
        return n == 0 ? null : String.valueOf(n);
    }

    @Override
    public void onRender3D(Render3DEvent event) {
        if (nullCheck()) return;
        ResourceKey<Level> dim = mc.level.dimension();
        float delta = event.getDelta();
        for (LogoutSpot spot : loggedPlayers.values()) {
            if (!dim.equals(spot.dimension)) continue;
            spot.capturedGeometry = WireframeEntityRenderer.render(
                    event.getMatrix(),
                    spot.entity,
                    spot.pos,
                    spot.capturedGeometry,
                    delta,
                    sideColor.getValue(),
                    spotColor.getValue(),
                    lineWidth.getValue()
            );
        }
    }

    @Override
    public void onRender2D(Render2DEvent event) {
        if (nullCheck()) return;
        if (MatrixCapture.projection == null) return;

        NametagsModule nametags = Homovore.moduleManager.getModuleByClass(NametagsModule.class);
        GuiGraphics graphics = event.getContext();
        ResourceKey<Level> dim = mc.level.dimension();

        for (LogoutSpot spot : loggedPlayers.values()) {
            if (!dim.equals(spot.dimension)) continue;
            double tagY = spot.pos.y + 1.8 + nametags.gap.getValue() * 0.5;
            double dist = mc.player.position().distanceTo(spot.pos);
            String timeStr = showTime.getValue() ? " " + formatElapsed(spot.logoutTime) : "";
            nametags.renderNametag(graphics, spot.pos.x, tagY, spot.pos.z, dist,
                    spot.name, nameColor.getValue().getRGB(), timeStr,
                    spot.totemPops, spot.armor, spot.mainHand, spot.offHand);
        }
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        resetState();
    }

    private void resetState() {
        loggedPlayers.clear();
        playerCache.clear();
        ticksOnPlayerList.clear();
        previousTabList.clear();
        previouslyVisible.clear();
        lastDimension = null;
        lastLevel = null;
    }

    private String formatElapsed(long logoutTime) {
        long secs = (System.currentTimeMillis() - logoutTime) / 1000;
        return String.format("%02d:%02d", secs / 60, secs % 60);
    }

    private static class SpotData {
        final String name;
        final UUID uuid;
        final Vec3 pos;
        final Player entity;
        final Map<EquipmentSlot, ItemStack> armor;
        final ItemStack mainHand;
        final ItemStack offHand;
        final int totemPops;

        SpotData(Player player) {
            this.name      = player.getGameProfile().name();
            this.uuid      = player.getUUID();
            this.pos       = player.position();
            this.entity    = player;
            this.totemPops = Homovore.playerInfoManager.getTotemPops(player.getUUID());
            this.mainHand  = player.getMainHandItem().copy();
            this.offHand   = player.getOffhandItem().copy();
            this.armor     = new EnumMap<>(EquipmentSlot.class);
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                armor.put(slot, player.getItemBySlot(slot).copy());
            }
        }
    }

    public static class LogoutSpot {
        public final String name;
        public final UUID uuid;
        public final Vec3 pos;
        public final Player entity;
        public final Map<EquipmentSlot, ItemStack> armor;
        public final ItemStack mainHand;
        public final ItemStack offHand;
        public final int totemPops;
        public final long logoutTime;
        public final ResourceKey<Level> dimension;

        public List<float[][]> capturedGeometry = null;

        LogoutSpot(SpotData data, ResourceKey<Level> dimension) {
            this.name       = data.name;
            this.uuid       = data.uuid;
            this.pos        = data.pos;
            this.entity     = data.entity;
            this.armor      = data.armor;
            this.mainHand   = data.mainHand;
            this.offHand    = data.offHand;
            this.totemPops  = data.totemPops;
            this.logoutTime = System.currentTimeMillis();
            this.dimension  = dimension;
        }
    }
}
