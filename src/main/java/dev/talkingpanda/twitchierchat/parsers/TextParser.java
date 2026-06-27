package dev.talkingpanda.twitchierchat.parsers;

import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;


public interface TextParser {

    String getStart();

    @Nullable String getEnd();

    @Nullable MutableComponent parse(String input);

    // Reverse the conversion to string done by the client when editing signs
    default String undo(String input) {
        return input;
    }
}

