package dev.talkingpanda.twitchierchat.parsers;

import dev.talkingpanda.twitchierchat.Emotes;
import net.minecraft.ChatFormatting;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.objects.AtlasSprite;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

public class EmoteParser implements TextParser {
    private static MutableComponent getEmote(String emote) {
        var emoteObject = new AtlasSprite(AtlasIds.GUI, Identifier.fromNamespaceAndPath("emotes", "emotes/" + emote));
        return Component.object(emoteObject).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.object(emoteObject).append(" " + emote))).withClickEvent(new ClickEvent.CopyToClipboard(":" + emote + ":")).withColor(ChatFormatting.WHITE).withoutShadow());
    }

    @Override
    public String getStart() {
        return ":";
    }

    @Override
    public @Nullable String getEnd() {
        return ":";
    }

    @Override
    public @Nullable MutableComponent parse(String input) {
        if (input.length() <= 2) return null;
        String emote = input.substring(1, input.length() - 1);
        if (!Emotes.emotes.containsKey(emote)) return null;

        return getEmote(emote);
    }

    @Override
    public String undo(String input) {
        return input.replaceAll("\\[emotes:emotes/(.*?)@gui]", ":$1:");
    }
}
