package dev.talkingpanda.twitchierchat.parsers;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

import static net.minecraft.util.Util.parseAndValidateUntrustedUri;

public class
ItalicParser implements TextParser{

    @Override
    public boolean comboable() {
        return true;
    }

    @Override
    public String getStart() {
        return "*";
    }

    @Override
    public @Nullable String getEnd() {
        return "*";
    }

    @Override
    public @Nullable MutableComponent parse(String input) {
        return parse(Component.literal(input.substring(1, input.length()-1)));
    }

    public @Nullable MutableComponent parse(MutableComponent input) {
        return input.setStyle(input.getStyle().withItalic(true));
    }
}
