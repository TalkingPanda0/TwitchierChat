package dev.talkingpanda.twitchierchat.parsers;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import org.jetbrains.annotations.Nullable;

import java.net.URI;

import static net.minecraft.util.Util.parseAndValidateUntrustedUri;

public class UrlParser implements TextParser{
    @Override
    public @Nullable MutableComponent parse(String input) {
        if(!input.startsWith("http")) {
            return null;
        }

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
