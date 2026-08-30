package dev.talkingpanda.twitchierchat.mixin;

import dev.talkingpanda.twitchierchat.Emotes;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import net.minecraft.server.network.ConfigurationTask;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import net.minecraft.server.network.config.ServerResourcePackConfigurationTask;
import org.jspecify.annotations.Nullable;
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

    @Shadow
    private @Nullable ConfigurationTask currentTask;

    @Inject(method = "addOptionalTasks", at = @At("TAIL"))
    public void dynamicResourcePack$getResourcePackProperties(CallbackInfo ci) throws Exception {
        var pack = Emotes.getPackProperties();
        if (pack == null) {
            return;
        }
        this.configurationTasks.add(new ServerResourcePackConfigurationTask(pack));
    }

    // Suppresses the error when the server has an existing resource pack, there probably is a better way
    @Inject(method = "handleResourcePackResponse", at = @At("HEAD"), cancellable = true)
    private void handleExtraResourcePackResponse(ServerboundResourcePackPacket packet, CallbackInfo ci) {
        if (this.currentTask != null && !this.currentTask.type().equals(ServerResourcePackConfigurationTask.TYPE) && packet.id().equals(Emotes.packId)) {
            ci.cancel();
        }
    }
}
