package dev.talkingpanda.twitchierchat.mixin;

import dev.talkingpanda.twitchierchat.Emotes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(DedicatedServer.class)
public class DedicatedServerMixin {
    @Inject(method = "getServerResourcePack", at = @At("HEAD"), cancellable = true)
    public void dynamicResourcePack$getResourcePackProperties(CallbackInfoReturnable<Optional<MinecraftServer.ServerResourcePackInfo>> cir) throws Exception {
        var pack = Emotes.getPackProperties();
        if(pack == null){
            return;
        }

        cir.setReturnValue(Optional.of(pack));
        cir.cancel();

    }
}
