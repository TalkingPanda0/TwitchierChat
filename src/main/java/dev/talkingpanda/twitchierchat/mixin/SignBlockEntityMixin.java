package dev.talkingpanda.twitchierchat.mixin;


import dev.talkingpanda.twitchierchat.TwitchierChat;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SignBlockEntity.class)
public class SignBlockEntityMixin {

    @ModifyVariable(method = "setText", at = @At("HEAD"), argsOnly = true, name = "text")
    private static SignText formatSignText(SignText text) {
        var messages = text.getMessages(false);
        for (int i = 0; i < messages.length; i++) {
            var message = messages[i];
            var formatted = TwitchierChat.formatText(message);
            if (formatted == null) continue;
            text = text.setMessage(i, formatted);

        }
        return text;
    }


}
