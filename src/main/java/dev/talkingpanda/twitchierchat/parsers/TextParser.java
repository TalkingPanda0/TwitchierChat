package dev.talkingpanda.twitchierchat.parsers;

import net.minecraft.network.chat.MutableComponent;
import org.jetbrains.annotations.Nullable;


public interface TextParser {

    String getStart();

    @Nullable String getEnd();

    @Nullable MutableComponent parse(String input);

    @Nullable
    default MutableComponent parse(MutableComponent input) {
        return input;
    }

    default boolean comboable() {
        return false;
    }

    // Reverse the conversion to string done by the client when editing signs
    default String undo(String input) {
        return input;
    }
}

