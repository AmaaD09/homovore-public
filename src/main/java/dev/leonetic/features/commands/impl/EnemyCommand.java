package dev.leonetic.features.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.leonetic.Homovore;
import dev.leonetic.features.commands.Command;
import dev.leonetic.manager.CommandManager;

import java.util.List;
import java.util.StringJoiner;

import static dev.leonetic.features.commands.argument.EnemyArgumentType.enemy;
import static dev.leonetic.features.commands.argument.EnemyArgumentType.getEnemy;
import static dev.leonetic.features.commands.argument.OnlinePlayerArgumentType.getOnlinePlayer;
import static dev.leonetic.features.commands.argument.OnlinePlayerArgumentType.onlinePlayer;

public class EnemyCommand extends Command {
    public EnemyCommand() {
        super("enemy", "enemies", "e");
        setDescription("Manages your enemies list");
    }

    @Override
    public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
        builder.then(literal("list")
                        .executes((ctx) -> {
                            List<String> enemies = Homovore.enemyManager.getEnemies();
                            if (enemies.isEmpty()) {
                                return success("You have no enemies :)");
                            }
                            StringJoiner joiner = new StringJoiner(",");
                            enemies.forEach(joiner::add);
                            return success("Enemies (%s): %s", enemies.size(), joiner);
                        }))
                .then(literal("clear")
                        .executes((ctx) -> {
                            Homovore.enemyManager.clearEnemies();
                            return success("Cleared enemies list");
                        }))
                .then(literal("add")
                        .then(argument("username", onlinePlayer())
                                .executes((ctx) -> {
                                    String username = getOnlinePlayer(ctx, "username");
                                    if (Homovore.enemyManager.isEnemy(username)) {
                                        return success("{red} %s {reset} is already on your enemies list.", username);
                                    }
                                    Homovore.enemyManager.addEnemy(username);
                                    return success("Added {red} %s {reset} to your enemies list", username);
                                })))
                .then(literal("remove")
                        .then(argument("username", enemy())
                                .executes((ctx) -> {
                                    String username = getEnemy(ctx, "username");
                                    if (!Homovore.enemyManager.isEnemy(username)) {
                                        return success("{red} %s {reset} is not on your enemies list.", username);
                                    }
                                    Homovore.enemyManager.removeEnemy(username);
                                    return success("Removed {red} %s {reset} from your enemies list", username);
                                })));
    }
}
