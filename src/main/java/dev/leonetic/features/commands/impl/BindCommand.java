package dev.leonetic.features.commands.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.leonetic.event.impl.input.KeyInputEvent;
import dev.leonetic.event.impl.input.MouseInputEvent;
import dev.leonetic.event.system.Subscribe;
import dev.leonetic.features.commands.Command;
import dev.leonetic.features.modules.Module;
import dev.leonetic.features.settings.Bind;
import dev.leonetic.manager.CommandManager;
import dev.leonetic.util.KeyboardUtil;

import static dev.leonetic.features.commands.argument.ModuleArgumentType.getModule;
import static dev.leonetic.features.commands.argument.ModuleArgumentType.module;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_UNKNOWN;

public class BindCommand extends Command {
    private Module module;

    public BindCommand() {
        super("bind", "setbind");
        setDescription("Sets a key bind for a module");
        EVENT_BUS.register(this);
    }

    @Override
    public void createArgumentBuilder(LiteralArgumentBuilder<CommandManager> builder) {
        builder.then(argument("module", module(true))
                .executes((ctx) -> {
                    module = getModule(ctx, "module");
                    return success("Press any key...");
                }));
    }

    @Subscribe
    public void onKey(final KeyInputEvent event) {
        if (nullCheck() || module == null || event.getKey() == GLFW_KEY_UNKNOWN || event.getAction() != 1) {
            return;
        }

        if (event.getKey() == GLFW_KEY_ESCAPE) {
            module = null;
            sendMessage("Operation canceled.");
            return;
        }

        sendMessage("Bind for {green} %s {} set to {green} %s",
                module.getName(),
                KeyboardUtil.getKeyName(event.getKey()));
        module.bind.setValue(new Bind(event.getKey()));
        module = null;
    }

    @Subscribe
    public void onMouse(final MouseInputEvent event) {
        if (nullCheck() || module == null || event.getAction() != 1) {
            return;
        }

        Bind bind = Bind.fromMouseButton(event.getButton());
        sendMessage("Bind for {green} %s {} set to {green} %s",
                module.getName(),
                bind.toString());
        module.bind.setValue(bind);
        module = null;
    }
}
