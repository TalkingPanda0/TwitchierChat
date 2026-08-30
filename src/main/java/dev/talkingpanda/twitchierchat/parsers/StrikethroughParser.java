package dev.talkingpanda.twitchierchat.parsers;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

public class
StrikethroughParser implements TextParser {

    @Override
    public boolean comboable() {
        return true;
    }

    @Override
    public String getStart() {
        return "~~";
    }

    @Override
    public @Nullable String getEnd() {
        return "~~";
    }

    @Override
    public @Nullable MutableComponent parse(String input) {
        return parse(Component.literal(input.substring(2, input.length() - 2)));
    }

    public @Nullable MutableComponent parse(MutableComponent input) {
        return input.setStyle(input.getStyle().withStrikethrough(true));
    }
}
