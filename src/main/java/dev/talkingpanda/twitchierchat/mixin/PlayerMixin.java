package dev.talkingpanda.twitchierchat.mixin;

import dev.talkingpanda.twitchierchat.Config;
import dev.talkingpanda.twitchierchat.NameColor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
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
