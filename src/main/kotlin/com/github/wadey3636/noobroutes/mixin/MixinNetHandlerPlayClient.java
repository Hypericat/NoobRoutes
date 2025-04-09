package com.github.wadey3636.noobroutes.mixin;

import me.defnotstolen.events.impl.TerminalOpenedEvent;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.S2DPacketOpenWindow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static me.defnotstolen.utils.Utils.postAndCatch;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    @Inject(method = "handleOpenWindow", at = @At("HEAD"))
    public void onHandleOpenWindow(S2DPacketOpenWindow packetIn, CallbackInfo ci) {
        postAndCatch(new TerminalOpenedEvent());

    }
}
