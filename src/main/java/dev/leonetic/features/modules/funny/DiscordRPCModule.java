package dev.leonetic.features.modules.funny;

import dev.leonetic.features.modules.Module;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;

import static java.lang.Math.abs;

public class DiscordRPCModule extends Module {
    private static final long APP_ID = 1497806002139955270L;

    private static final RichPresence rpc = new RichPresence();
    private int ticks;
    private int antileak = 50000;

    public DiscordRPCModule() {
        super("DiscordRPC", "Shows your dimension and coords as Discord Rich Presence.", Category.FUNNY);
    }

    @Override
    public void onEnable() {
        DiscordIPC.start(APP_ID, null);
        rpc.setStart(System.currentTimeMillis() / 1000L);
        rpc.setLargeImage("icon", "Homovore");
        ticks = 0;
    }

    @Override
    public void onDisable() {
        DiscordIPC.stop();
    }

    @Override
    public void onTick() {
        if (++ticks < 20) return;
        ticks = 0;

        if (nullCheck()) return;

        String dimPath = mc.player.level().dimension().identifier().getPath();
        String dimName = switch (dimPath) {
            case "overworld" -> "Overworld";
            case "the_nether" -> "The Nether";
            case "the_end" -> "The End";
            default -> dimPath;
        };

        rpc.setDetails("In " + dimName);

        if (abs(mc.player.getBlockX()) > antileak || abs(mc.player.getBlockZ()) > antileak) {
            rpc.setState(String.format("Coords: x x x"));
        } else {
            rpc.setState(String.format("Coords: %d %d %d",
                    mc.player.getBlockX(),
                    mc.player.getBlockY(),
                    mc.player.getBlockZ()));
        }

        DiscordIPC.setActivity(rpc);
    }
}
