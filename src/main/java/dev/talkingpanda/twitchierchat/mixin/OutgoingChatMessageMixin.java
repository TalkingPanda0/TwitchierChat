package dev.talkingpanda.twitchierchat.mixin;

import dev.talkingpanda.twitchierchat.Pings;
import dev.talkingpanda.twitchierchat.TwitchierChat;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(OutgoingChatMessage.class)
public interface OutgoingChatMessageMixin {
    @ModifyVariable(method = "create", at = @At("HEAD"), argsOnly = true, name = "message")
    private static PlayerChatMessage formatChatMessage(PlayerChatMessage message) {
        Pings.handlePings(message);
        if (message.unsignedContent() != null) return message;

        var formatted = TwitchierChat.formatString(message.signedContent());
        if(formatted == null) return message;
        return message.withUnsignedContent(formatted);
    }
}
