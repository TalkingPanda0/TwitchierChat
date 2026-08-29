package dev.talkingpanda.twitchierchat.parsers;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

import static net.minecraft.util.Util.parseAndValidateUntrustedUri;

public class
UnderlineParser implements TextParser{

    @Override
    public boolean comboable() {
        return true;
    }

    @Override
    public String getStart() {
        return "__";
    }

    @Override
    public @Nullable String getEnd() {
        return "__";
    }

    @Override
    public @Nullable MutableComponent parse(String input) {
        return parse(Component.literal(input.substring(2, input.length()-2)));
    }

    public @Nullable MutableComponent parse(MutableComponent input) {
        return input.setStyle(input.getStyle().withUnderlined(true));
    }
}
