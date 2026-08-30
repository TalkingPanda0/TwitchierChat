package dev.talkingpanda.twitchierchat.mixin;

import dev.talkingpanda.twitchierchat.TwitchierChat;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(AnvilMenu.class)
public class AnvilMenuMixin {
    @Redirect(method = "createResult", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/chat/Component;literal(Ljava/lang/String;)Lnet/minecraft/network/chat/MutableComponent;"))
    private MutableComponent getNewItemName(String text) {
        var formatted = TwitchierChat.formatString(text);
        if (formatted == null) return Component.literal(text);
        return formatted;
    }
}
