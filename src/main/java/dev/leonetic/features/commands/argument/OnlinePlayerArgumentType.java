package dev.leonetic.features.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.concurrent.CompletableFuture;

public class OnlinePlayerArgumentType implements ArgumentType<String> {
    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        ClientPacketListener conn = Minecraft.getInstance().getConnection();
        if (conn == null) {
            return builder.buildFuture();
        }

        String input = builder.getRemainingLowerCase();
        for (var info : conn.getOnlinePlayers()) {
            String name = info.getProfile().name();
            if (name == null) continue;
            if (input.isEmpty() || name.toLowerCase().contains(input)) {
                builder.suggest(name);
            }
        }

        return builder.buildFuture();
    }

    public static OnlinePlayerArgumentType onlinePlayer() {
        return new OnlinePlayerArgumentType();
    }

    public static String getOnlinePlayer(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, String.class);
    }
}
