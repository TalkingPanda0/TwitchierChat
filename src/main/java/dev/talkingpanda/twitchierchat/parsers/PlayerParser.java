package dev.talkingpanda.twitchierchat.parsers;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.world.item.component.ResolvableProfile;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;

public class PlayerParser implements TextParser{

    private static MutableComponent getPlayerHead(String player) {
        var headObject = new PlayerSprite(ResolvableProfile.createUnresolved(player), true);
        return Component.object(headObject).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.object(headObject).append(" " + player))).withClickEvent(new ClickEvent.CopyToClipboard("<" + player + ">")).withColor(ChatFormatting.WHITE));
    }


    @Override
    public String getStart() {
        return "<";
    }

    @Override
    public @Nullable String getEnd() {
        return ">";
    }

    @Override
    public @Nullable MutableComponent parse(String input) {
        String name = input.substring(1,input.length()-1);
        if (!Pattern.compile("^\\w{3,16}$").matcher(name).matches()) return null;
        return getPlayerHead(name);
    }

    @Override
    public String undo(String input) {
        return input.replaceAll("\\[(.*?) head]", "<$1>");
    }
}
