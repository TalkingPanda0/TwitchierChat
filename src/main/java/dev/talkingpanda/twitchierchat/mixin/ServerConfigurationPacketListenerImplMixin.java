package dev.talkingpanda.twitchierchat.mixin;

import dev.talkingpanda.twitchierchat.Emotes;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.config.ServerResourcePackConfigurationTask;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Queue;

@Mixin(ServerConfigurationPacketListenerImpl.class)
public class ServerConfigurationPacketListenerImplMixin {
    @Shadow
    @Final
    private Queue<ConfigurationTask> configurationTasks;

    @Inject(method = "addOptionalTasks", at = @At("TAIL"))
    public void dynamicResourcePack$getResourcePackProperties(CallbackInfo ci) throws Exception {
        var pack = Emotes.getPackProperties();
        if(pack == null){
            return;
        }
        this.configurationTasks.add(new ServerResourcePackConfigurationTask(pack));
    }
}
