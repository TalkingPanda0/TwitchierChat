package dev.talkingpanda.twitchierchat.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.authlib.GameProfile;
import dev.talkingpanda.twitchierchat.Replies;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.OutgoingChatMessage;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayer.class)
public abstract  class ServerPlayerMixin extends Player {

    public ServerPlayerMixin(Level level, GameProfile gameProfile) {
        super(level, gameProfile);
    }

    @ModifyVariable(method = "sendChatMessage", at = @At(value = "HEAD"), argsOnly = true, name = "message")
    private static OutgoingChatMessage formatChatMessage(OutgoingChatMessage message, @Local(argsOnly = true, name = "chatType") ChatType.Bound chatType) {
        return Replies.handleOutgoingMessage(message,chatType);
    }

    @Inject(method = "getTabListDisplayName",at = @At("RETURN"),cancellable = true)
    private void colorTabListName(CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(this.getName());
    }
}
