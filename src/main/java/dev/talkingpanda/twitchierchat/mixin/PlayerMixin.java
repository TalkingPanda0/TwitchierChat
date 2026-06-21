package dev.talkingpanda.twitchierchat.mixin;

import dev.talkingpanda.twitchierchat.Config;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin  extends LivingEntity {


    protected PlayerMixin(EntityType<? extends LivingEntity> type, Level level) {
        super(type, level);
    }

    @Inject(method = "getName",at = @At("RETURN"),cancellable = true)
    public void colorName(CallbackInfoReturnable<Component> cir) {
        Integer color = Config.getColor(this.getUUID());
        if(color == null) {
            return;
        }
        cir.setReturnValue(cir.getReturnValue().copy().withColor(color));
    }
}
