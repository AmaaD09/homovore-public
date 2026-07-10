package dev.leonetic.features.commands.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.leonetic.Homovore;
import dev.leonetic.features.commands.Command;
import dev.leonetic.manager.CommandManager;
import dev.leonetic.manager.FriendManager;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;

import static dev.leonetic.features.commands.argument.FriendArgumentType.friend;
import static dev.leonetic.features.commands.argument.FriendArgumentType.getFriend;
import static dev.leonetic.features.commands.argument.OnlinePlayerArgumentType.getOnlinePlayer;
import static dev.leonetic.features.commands.argument.OnlinePlayerArgumentType.onlinePlayer;

public class FriendCommand extends Command {
    private static final long DAY_MILLIS = 24L * 60L * 60L * 1000L;

    public FriendCommand() {
        super("friend", "friends", "f");
        setDescription("Manages your friends list");
    }

    @Override
    public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
        builder.then(literal("list")
                        .executes((ctx) -> {
                            List<String> friends = Homovore.friendManager.getFriends();
                            if (friends.isEmpty()) {
                                return success("You have no friends :(");
                            }
                            StringJoiner joiner = new StringJoiner(", ");
                            for (String name : friends) {
                                long expiry = Homovore.friendManager.getExpiry(name);
                                if (expiry == FriendManager.PERMANENT) {
                                    joiner.add(name);
                                } else {
                                    joiner.add(name + " (" + formatRemaining(expiry) + ")");
                                }
                            }
                            return success("Friends (%s): %s", friends.size(), joiner);
                        }))
                .then(literal("clear")
                        .executes((ctx) -> {
                            Homovore.friendManager.clearFriends();
                            return success("Cleared friends list");
                        }))
                .then(literal("add")
                        .then(argument("username", onlinePlayer())
                                .executes((ctx) -> addPlayer(getOnlinePlayer(ctx, "username"), FriendManager.PERMANENT))))
                .then(literal("remove")
                        .then(argument("username", friend())
                                .executes((ctx) -> {
                                    String username = getFriend(ctx, "username");
                                    if (!Homovore.friendManager.isFriend(username)) {
                                        return success("{green} %s {reset} is not on your friends list.", username);
                                    }
                                    Homovore.friendManager.removeFriend(username);
                                    return success("Removed {green} %s {reset} from your friends list", username);
                                })))

                .then(literal("radius")
                        .then(literal("add")
                                .then(argument("blocks", IntegerArgumentType.integer(1))
                                        .executes((ctx) -> addRadius(IntegerArgumentType.getInteger(ctx, "blocks"), FriendManager.PERMANENT)))))

                .then(literal("temp")
                        .then(argument("days", IntegerArgumentType.integer(1))
                                .then(literal("add")
                                        .then(argument("username", onlinePlayer())
                                                .executes((ctx) -> addPlayer(getOnlinePlayer(ctx, "username"), expiryFromDays(IntegerArgumentType.getInteger(ctx, "days"))))))
                                .then(literal("radius")
                                        .then(literal("add")
                                                .then(argument("blocks", IntegerArgumentType.integer(1))
                                                        .executes((ctx) -> addRadius(IntegerArgumentType.getInteger(ctx, "blocks"), expiryFromDays(IntegerArgumentType.getInteger(ctx, "days")))))))));
    }

    private int addPlayer(String username, long expiry) {
        if (Homovore.friendManager.isFriend(username)) {
            return success("{green} %s {reset} is already on your friends list.", username);
        }
        Homovore.friendManager.addFriend(username, expiry);
        if (expiry == FriendManager.PERMANENT) {
            return success("Added {green} %s {reset} to your friends list", username);
        }
        return success("Added {green} %s {reset} to your friends list for %s", username, formatRemaining(expiry));
    }

    private int addRadius(int blocks, long expiry) {
        if (nullCheck()) {
            return fail("You need to be in a world to do that.");
        }
        double radiusSq = (double) blocks * blocks;
        UUID self = mc.player.getUUID();
        int added = 0;
        for (Player player : mc.level.players()) {
            if (player.getUUID().equals(self)) continue;
            if (mc.player.distanceToSqr(player) > radiusSq) continue;
            String name = player.getGameProfile().name();
            if (name == null) continue;
            if (Homovore.friendManager.isFriend(name)) continue;
            Homovore.friendManager.addFriend(name, expiry);
            added++;
        }
        if (added == 0) {
            return success("No new players within %s blocks.", blocks);
        }
        if (expiry == FriendManager.PERMANENT) {
            return success("Added %s player(s) within %s blocks to your friends list", added, blocks);
        }
        return success("Added %s player(s) within %s blocks to your friends list for %s", added, blocks, formatRemaining(expiry));
    }

    private static long expiryFromDays(int days) {
        return System.currentTimeMillis() + days * DAY_MILLIS;
    }

    private static String formatRemaining(long expiry) {
        long ms = Math.max(0L, expiry - System.currentTimeMillis());
        long hours = ms / (60L * 60L * 1000L);
        long days = hours / 24L;
        hours %= 24L;
        if (days > 0) {
            return days + "d " + hours + "h";
        }
        long minutes = (ms / (60L * 1000L)) % 60L;
        if (hours > 0) {
            return hours + "h " + minutes + "m";
        }
        return minutes + "m";
    }
}
