/*package com.github.wadey3636.noobroutes.mixin;

import io.netty.channel.ChannelHandlerContext;
import com.github.wadey3636.noobroutes.events.impl.PacketEvent;
import com.github.wadey3636.noobroutes.utils.ClientUtils;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static com.github.wadey3636.noobroutes.utils.UtilsKt.postAndCatch;


}*/

package com.github.wadey3636.noobroutes.mixin;

import io.netty.channel.ChannelHandlerContext;
import com.github.wadey3636.noobroutes.events.impl.PacketEvent;
import com.github.wadey3636.noobroutes.utils.ServerUtils;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


import static com.github.wadey3636.noobroutes.utils.UtilsKt.postAndCatch;

@Mixin(value = {NetworkManager.class}, priority = 1003)
public class MixinNetworkManager {

    @Inject(method = "channelRead0*", at = @At("HEAD"), cancellable = true)
    private void onReceivePacket(ChannelHandlerContext context, Packet<?> packet, CallbackInfo ci) {
        if (postAndCatch(new PacketEvent.Receive(packet)) && !ci.isCancelled()) ci.cancel();
    }


    @Inject(method = {"sendPacket(Lnet/minecraft/network/Packet;)V"}, at = {@At("HEAD")}, cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        if (!ServerUtils.handleSendPacket(packet))
            if (postAndCatch(new PacketEvent.Send(packet)) && !ci.isCancelled()) ci.cancel();
    }
}

