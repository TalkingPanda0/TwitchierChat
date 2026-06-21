package dev.talkingpanda.twitchierchat.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import dev.talkingpanda.twitchierchat.Emotes;
import dev.talkingpanda.twitchierchat.Replies;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @ModifyVariable(method = "broadcastChatMessage(Lnet/minecraft/network/chat/PlayerChatMessage;Ljava/util/function/Predicate;Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/network/chat/ChatType$Bound;)V", at = @At("STORE"), name = "tracked")
    private static OutgoingChatMessage formatChatMessage(OutgoingChatMessage message, @Local(argsOnly = true, name = "chatType") ChatType.Bound chatType) {
        return Replies.handleOutgoingMessage(message,chatType);
    }

    @Inject(method = "placeNewPlayer",at = @At("TAIL"))
    private void sendEmotes(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        player.connection.send(new ClientboundPlayerInfoUpdatePacket(EnumSet.of( ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER),Emotes.emotes.values()));
    }
}