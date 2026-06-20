package dev.leonetic.manager;

import dev.leonetic.event.impl.entity.TotemPopEvent;
import dev.leonetic.event.impl.entity.player.PreTickEvent;
import dev.leonetic.event.impl.network.DisconnectEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.Feature;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerInfoManager extends Feature {

    private final Map<UUID, Integer> totemPops = new ConcurrentHashMap<>();
    private final Set<UUID> lastTickAlive = ConcurrentHashMap.newKeySet();

    public void init() {
        EVENT_BUS.register(this);
    }

    @Subscribe
    public void onTotemPop(TotemPopEvent event) {
        totemPops.merge(event.getEntity().getUUID(), 1, Integer::sum);
    }

    @Subscribe
    public void onPreTick(PreTickEvent event) {
        if (mc.level == null || mc.player == null) return;

        Set<UUID> currentlyAlive = new HashSet<>();
        Set<UUID> presentIds = new HashSet<>();
        for (Player player : mc.level.players()) {
            UUID id = player.getUUID();
            presentIds.add(id);
            if (player.isDeadOrDying()) {
                if (lastTickAlive.contains(id)) {
                    totemPops.remove(id);
                }
            } else {
                currentlyAlive.add(id);
            }
        }

        totemPops.keySet().removeIf(id -> !presentIds.contains(id));
        lastTickAlive.clear();
        lastTickAlive.addAll(currentlyAlive);
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        resetAll();
    }

    public int getTotemPops(UUID uuid) {
        return totemPops.getOrDefault(uuid, 0);
    }

    public void resetTotemPops(UUID uuid) {
        totemPops.remove(uuid);
    }

    public void resetAll() {
        totemPops.clear();
        lastTickAlive.clear();
    }

    public ItemStack getEquipment(Player player, EquipmentSlot slot) {
        return player.getItemBySlot(slot);
    }

    public ItemStack getMainHandItem(Player player) {
        return player.getMainHandItem();
    }

    public ItemStack getOffHandItem(Player player) {
        return player.getOffhandItem();
    }
}
