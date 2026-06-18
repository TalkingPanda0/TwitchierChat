package dev.talkingpanda.twitchierchat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ChatColorArgument implements ArgumentType<Integer> {
    public static final DynamicCommandExceptionType ERROR_INVALID_HEX = new DynamicCommandExceptionType(value -> Component.literal("Invalid hex " + value));
    public static final DynamicCommandExceptionType ERROR_INVALID_COLOR = new DynamicCommandExceptionType(value -> Component.literal("Invalid color " + value));
    private static final Map<String, Integer> COLORS = Map.ofEntries(
            Map.entry("red", 0xFF0000),
            Map.entry("blue", 0x0000FF),
            Map.entry("green", 0x008000),
            Map.entry("firebrick", 0xb22222),
            Map.entry("coral", 0xFF7F50),
            Map.entry("yellowgreen", 0x9acd32),
            Map.entry("orangered", 0xFF4500),
            Map.entry("seagreen", 0x2e8b57),
            Map.entry("goldenrod", 0xdaa520),
            Map.entry("chocolate", 0xd2691e),
            Map.entry("cadetblue", 0x5f9ea0),
            Map.entry("dodgerblue", 0x1e90ff),
            Map.entry("hotpink", 0xff69b4),
            Map.entry("blueviolet", 0x8a2be2),
            Map.entry("springgreen", 0x00ff7f)
    );

    public static ChatColorArgument color() {
        return new ChatColorArgument();
    }

    public static Integer getColor(CommandContext<CommandSourceStack> context, String name) {
        return context.getArgument(name, Integer.class);
    }


    @Override
    public Integer parse(StringReader reader) throws CommandSyntaxException {
        String input = reader.readUnquotedString();
        if (input.startsWith("#")) {
            input = input.substring(1);
            try {
                return Integer.parseInt(input, 16);
            } catch (NumberFormatException e) {
                throw ERROR_INVALID_HEX.createWithContext(reader, input);
            }
        }

        Integer color = COLORS.get(input);

        if (color == null) {
            throw ERROR_INVALID_COLOR.createWithContext(reader, input);
        }

        return color;
    }


    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(COLORS.keySet(), builder);
    }

    @Override
    public Collection<String> getExamples() {
        return COLORS.keySet();
    }
}
