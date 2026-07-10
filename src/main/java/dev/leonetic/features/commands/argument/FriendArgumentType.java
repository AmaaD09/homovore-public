package dev.leonetic.features.commands.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.leonetic.Homovore;

import java.util.concurrent.CompletableFuture;

public class FriendArgumentType implements ArgumentType<String> {
    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        return reader.readString();
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        String input = builder.getRemainingLowerCase();
        for (String name : Homovore.friendManager.getFriends()) {
            if (input.isEmpty() || name.toLowerCase().contains(input)) {
                builder.suggest(name);
            }
        }
        return builder.buildFuture();
    }

    public static FriendArgumentType friend() {
        return new FriendArgumentType();
    }

    public static String getFriend(CommandContext<?> ctx, String name) {
        return ctx.getArgument(name, String.class);
    }
}
