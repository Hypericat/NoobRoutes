/*package com.github.wadey3636.noobroutes.mixin;

import io.netty.channel.ChannelHandlerContext;
import me.odinmain.events.impl.PacketEvent;
import me.odinmain.utils.ServerUtils;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.odinmain.utils.Utils.postAndCatch;

@Mixin(value = {NetworkManager.class}, priority = 1001)
public class MixinNetworkManager {

    @Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true)
    private void onReceivePacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (postAndCatch(new PacketEvent.Receive(packet)) && !ci.isCancelled()) ci.cancel();
    }


    @Inject(method = {"sendPacket(Lnet/minecraft/network/Packet;)V"}, at = {@At("HEAD")}, cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (MinecraftForge.EVENT_BUS.post(new PacketEvent.Send(packet)))
            ci.cancel();
    }
}*/

package com.github.wadey3636.noobroutes.mixin;

import io.netty.channel.ChannelHandlerContext;
import me.defnotstolen.events.impl.PacketEvent;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetworkManager.class, priority = 1001)
public class MixinNetworkManager {

    // Handles incoming packets (S2C - Server to Client)
    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
    private void onReceivePacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        // Post the PacketEvent.Receive event and cancel if necessary
        if (MinecraftForge.EVENT_BUS.post(new PacketEvent.Receive(packet))) {
            ci.cancel();
        }
    }

    // Handles outgoing packets (C2S - Client to Server)
    @Inject(method = "sendPacket", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        // Post the PacketEvent.Send event and cancel if necessary
        if (MinecraftForge.EVENT_BUS.post(new PacketEvent.Send(packet))) {
            ci.cancel();
        }
    }
}

