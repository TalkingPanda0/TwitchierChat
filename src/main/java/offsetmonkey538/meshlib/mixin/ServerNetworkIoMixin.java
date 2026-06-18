package offsetmonkey538.meshlib.mixin;

import io.netty.channel.Channel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import offsetmonkey538.meshlib.impl.ProtocolHandler;

import static offsetmonkey538.meshlib.impl.ProtocolHandler.MOD_ID;

@Mixin(targets = "net.minecraft.server.network.ServerConnectionListener$1")
public abstract class ServerNetworkIoMixin {

    @Inject(
            method = "initChannel(Lio/netty/channel/Channel;)V",
            at = @At("TAIL")
    )
    private void addHttpHandler(Channel channel, CallbackInfo ci) {
        channel.pipeline().addFirst(MOD_ID, new ProtocolHandler());
    }
}