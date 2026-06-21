package dev.talkingpanda.twitchierchat.mixin;

import dev.talkingpanda.twitchierchat.Emotes;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumSet;

@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Inject(method = "placeNewPlayer",at = @At("TAIL"))
    private void sendEmotes(Connection connection, ServerPlayer player, CommonListenerCookie cookie, CallbackInfo ci) {
        player.connection.send(new ClientboundPlayerInfoUpdatePacket(EnumSet.of( ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER),Emotes.emotes.values()));
    }
}