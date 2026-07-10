package dev.leonetic.features.modules.client;

import dev.leonetic.Homovore;
import dev.leonetic.event.impl.ClientEvent;
import dev.leonetic.event.impl.entity.TotemPopEvent;
import dev.leonetic.event.impl.entity.player.PreTickEvent;
import dev.leonetic.event.impl.network.DisconnectEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.modules.Module;
import dev.leonetic.features.modules.hud.NotifierHudModule;
import dev.leonetic.features.settings.Setting;
import dev.leonetic.util.TextUtil;
import dev.leonetic.util.player.ChatUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationsModule extends Module {
    private static final String MODULE_FORMAT = "%s %s %s";

    private static final int INFO_COLOR = 0xFFFF55;

    public Setting<Boolean> moduleToggle = bool("Module Toggle", true);
    public Setting<Boolean> deaths = bool("Deaths", true);
    public Setting<Boolean> totemPops = bool("TotemPops", true);
    public Setting<Boolean> gear = bool("Gear", false);

    private final Set<UUID> lastTickAlive = ConcurrentHashMap.newKeySet();

    private record GearItem(Item item, String label, Setting<Integer> threshold) {}

    private final GearItem[] gearItems = {
            new GearItem(Items.OBSIDIAN, "obsidian", gearThreshold("Obsidian", 128, 512)),
            new GearItem(Items.EXPERIENCE_BOTTLE, "XP bottles", gearThreshold("XP Bottles", 128, 512)),
            new GearItem(Items.END_CRYSTAL, "crystals", gearThreshold("Crystals", 128, 512)),
            new GearItem(Items.ENDER_PEARL, "pearls", gearThreshold("Pearls", 32, 256)),
    };

    private Setting<Integer> gearThreshold(String label, int def, int max) {
        Setting<Integer> s = num(label + " Threshold", def, 0, max);
        s.setVisibility(v -> gear.getValue());
        return s;
    }

    private final Set<Item> gearWarned = ConcurrentHashMap.newKeySet();

    public NotificationsModule() {
        super("Notifications", "Displays notifications for various client events", Category.CLIENT);
    }

    @Subscribe
    public void onClient(ClientEvent event) {
        if (!moduleToggle.getValue()
                || event.getType() != ClientEvent.Type.TOGGLE_MODULE
                || event.getFeature() instanceof ClickGuiModule) {
            return;
        }

        boolean moduleState = event.getFeature().isEnabled();
        String name = event.getFeature().getName();
        ChatUtil.sendPersistent("module:" + name, TextUtil.text(MODULE_FORMAT,
                name,
                moduleState ? "{green}" : "{red}",
                moduleState ? "enabled" : "disabled"));
    }

    @Subscribe(priority = 100)
    public void onPreTick(PreTickEvent event) {
        if (!moduleToggle.getValue() || !deaths.getValue()) return;
        if (mc.level == null || mc.player == null) return;

        Set<UUID> currentlyAlive = new HashSet<>();
        for (Player player : mc.level.players()) {
            UUID id = player.getUUID();
            if (player.isDeadOrDying()) {
                if (lastTickAlive.contains(id)) {
                    int pops = Homovore.playerInfoManager.getTotemPops(id);
                    String name = player.getGameProfile().name();
                    String unit = pops == 1 ? "totem" : "totems";
                    Component message = Component.empty()
                            .append(Component.literal(name + " has died after popping "))
                            .append(Component.literal(String.valueOf(pops)).withColor(0xFFFF55))
                            .append(Component.literal(" " + unit + "."));
                    ChatUtil.sendPrefixed(ChatUtil.getSkullComponent(), message);
                }
            } else {
                currentlyAlive.add(id);
            }
        }
        lastTickAlive.clear();
        lastTickAlive.addAll(currentlyAlive);
    }

    @Subscribe(priority = -100)
    public void onTotemPop(TotemPopEvent event) {
        if (!moduleToggle.getValue() || !totemPops.getValue()) return;
        if (mc.player == null) return;

        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Player player)) return;

        UUID id = player.getUUID();
        int pops = Homovore.playerInfoManager.getTotemPops(id);
        String unit = pops == 1 ? "totem" : "totems";
        boolean self = player == mc.player;
        String subject = self ? "You have" : player.getGameProfile().name() + " has";

        Component body = Component.empty()
                .append(Component.literal(subject + " popped "))
                .append(Component.literal(String.valueOf(pops)).withColor(0xFFFF55))
                .append(Component.literal(" " + unit + "!"));

        ChatUtil.sendPersistentPrefixed(
                "pops:" + id,
                ChatUtil.getInfoComponent(),
                body);

        NotifierHudModule.push(
                "pops:" + id,
                NotifierHudModule.Icon.INFO,
                body);
    }

    @Subscribe
    public void onGearTick(PreTickEvent event) {
        if (!moduleToggle.getValue() || !gear.getValue()) return;
        if (mc.player == null) return;

        for (GearItem gi : gearItems) {
            int count = countItem(gi.item());
            int threshold = gi.threshold().getValue();

            if (count >= threshold) {
                gearWarned.remove(gi.item());
                continue;
            }

            if (!gearWarned.add(gi.item())) continue;

            Component body = Component.empty()
                    .append(Component.literal("Low on " + gi.label() + ": "))
                    .append(Component.literal(String.valueOf(count)).withColor(INFO_COLOR))
                    .append(Component.literal(" left"));
            NotifierHudModule.push("gear:" + gi.label(), NotifierHudModule.Icon.INFO, body);
        }
    }

    private int countItem(Item item) {
        int total = 0;
        Inventory inv = mc.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) total += s.getCount();
        }
        return total;
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        lastTickAlive.clear();
        gearWarned.clear();
    }
}
