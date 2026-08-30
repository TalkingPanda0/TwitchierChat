package dev.talkingpanda.twitchierchat.mixin;


import dev.talkingpanda.twitchierchat.Pings;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.network.chat.PlayerChatMessage;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OutgoingChatMessage.Player.class)
public class OutgoingChatMessagePlayerMixin {
    @Shadow
    @Final
    private PlayerChatMessage message;

    @Inject(method = "sendToPlayer", at = @At("TAIL"))
    private void inject(ServerPlayer player, boolean filtered, ChatType.Bound chatType, CallbackInfo ci) {
        Pings.handlePings(this.message, player);
    }
}
