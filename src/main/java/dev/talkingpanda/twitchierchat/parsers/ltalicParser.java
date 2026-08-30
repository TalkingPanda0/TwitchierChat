package dev.talkingpanda.twitchierchat.parsers;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;

public class
ltalicParser implements TextParser {

    @Override
    public boolean comboable() {
        return true;
    }

    @Override
    public String getStart() {
        return "***";
    }

    @Override
    public @Nullable String getEnd() {
        return "***";
    }

    @Override
    public @Nullable MutableComponent parse(String input) {
        return parse(Component.literal(input.substring(3, input.length() - 3)));
    }

    public @Nullable MutableComponent parse(MutableComponent input) {
        return input.setStyle(input.getStyle().withItalic(true).withBold(true));
    }
}
