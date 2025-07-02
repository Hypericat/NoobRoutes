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

package mixin;

import net.minecraft.client.settings.KeyBinding;
import noobroutes.features.move.AutoPath;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBinding.class)
public class MixinKeybinding {
    @Shadow private int keyCode;

    @Shadow private boolean pressed;

    @Shadow private int pressTime;

    @Inject(method = "isPressed", at = @At("HEAD"), cancellable = true)
    private void isPressed(CallbackInfoReturnable<Boolean> cir) {
        if (/*AutoPath.INSTANCE.shouldCancelKey(keyCode)*/ false)  {
            cir.setReturnValue(false);
            this.pressed = false;
            this.pressTime = 0;
        }
    }
}

