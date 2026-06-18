package dev.talkingpanda.twitchierchat.mixin;

import dev.talkingpanda.twitchierchat.Emotes;
import dev.talkingpanda.twitchierchat.TwitchierChat;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
    @Redirect(method = "createResult", at = @At(value = "INVOKE", target= "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"))
    private MutableComponent getNewItemName(String string) {
        var formatted = TwitchierChat.formatString(string);
        if(formatted == null) return Component.literal(string);
        return formatted;
    }
}
