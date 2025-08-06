/*package noobroutes.mixin;

import io.netty.channel.ChannelHandlerContext;
import noobroutes.events.impl.PacketEvent;
import noobroutes.utils.ClientUtils;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noobroutes.utils.UtilsKt.postAndCatch;


}*/

package noobroutes.mixin;

import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import noobroutes.INetwork;
import noobroutes.events.impl.PacketEvent;
import noobroutes.events.impl.PacketReturnEvent;
import noobroutes.utils.ServerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noobroutes.utils.UtilsKt.postAndCatch;

@Mixin(value = {NetworkManager.class}, priority = 1003)
public class MixinNetworkManager implements INetwork {

    @Shadow private INetHandler packetListener;

    @Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true)
    private void noobRoutes$onReceivePacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (postAndCatch(new PacketEvent.Receive(packet)) && !ci.isCancelled()) ci.cancel();
    }


    @Inject(method = {"sendPacket(Lnet/minecraft/network/Packet;)V"}, at = {@At("HEAD")}, cancellable = true)
    private void noobRoutes$onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (!ServerUtils.handleSendPacket(packet))
            if (postAndCatch(new PacketEvent.Send(packet)) && !ci.isCancelled()) ci.cancel();
    }

    @Inject(method = {"sendPacket(Lnet/minecraft/network/Packet;)V"}, at = {@At("RETURN")})
    private void noobRoutes$onSendPacketReturn(Packet<?> packet, CallbackInfo ci) {
        postAndCatch(new PacketReturnEvent.Send(packet));
    }

    @Inject(method = "channelRead0*", at = {@At("RETURN")})
    private void noobRoutes$onReceivePacketReturn(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        postAndCatch(new PacketReturnEvent.Receive(packet));
    }


    @Override
    public void noobRoutes$receive(Packet<INetHandler> packet) {
        packet.processPacket(this.packetListener);
    }
}

