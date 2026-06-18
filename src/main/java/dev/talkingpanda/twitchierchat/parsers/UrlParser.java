package dev.talkingpanda.twitchierchat.parsers;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.regex.Pattern;

import static net.minecraft.util.Util.parseAndValidateUntrustedUri;

public class UrlParser implements TextParser{
    private static final Pattern urlRegex = Pattern.compile("https?://(www\\.)?[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b([-a-zA-Z0-9()@:%_+.~#?&/=]*)");
    @Override
    public @Nullable MutableComponent parse(String input) {
        URI uri;

        try {
            uri = parseAndValidateUntrustedUri(input);
        } catch (Exception e) {
            return null;
        }

        return Component.literal(input).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal("Open the link"))).withClickEvent(new ClickEvent.OpenUrl(uri))
                .withColor(ChatFormatting.AQUA)
                .withItalic(true));
    }
}
