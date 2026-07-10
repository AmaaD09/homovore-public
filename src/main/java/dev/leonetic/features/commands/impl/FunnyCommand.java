package dev.leonetic.features.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.leonetic.Homovore;
import dev.leonetic.features.Feature;
import dev.leonetic.features.commands.Command;
import dev.leonetic.features.gui.HomovoreGui;
import dev.leonetic.features.modules.Module;
import dev.leonetic.manager.CommandManager;

public class FunnyCommand extends Command {
    public FunnyCommand() {
        super("funny");
        setDescription("Toggles visibility of the Funny category in the ClickGui");
    }

    @Override
    public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
        builder.executes((ctx) -> {
            CommandManager cm = ctx.getSource();
            boolean nowVisible = !cm.isFunnyVisible();
            cm.setFunnyVisible(nowVisible);
            if (!nowVisible) {
                Homovore.moduleManager.stream()
                        .filter(m -> m.getCategory() == Module.Category.FUNNY)
                        .filter(Feature::isEnabled)
                        .forEach(Module::disable);
            }
            HomovoreGui.getInstance().reload();
            return success("Funny category is now %s",
                    nowVisible ? "{green} visible" : "{red} hidden");
        });
    }
}
