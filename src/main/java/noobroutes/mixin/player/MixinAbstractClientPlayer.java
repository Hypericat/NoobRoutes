package noobroutes.mixin.player;


import net.minecraft.client.entity.AbstractClientPlayer;
import noobroutes.features.render.FreeCam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({AbstractClientPlayer.class})
public abstract class MixinAbstractClientPlayer{
    @Inject(
            at = {@At("HEAD")},
            method = {"isSpectator()Z"},
            cancellable = true
    )
    private void isSpectator(CallbackInfoReturnable<Boolean> cir) {
        if (FreeCam.INSTANCE.shouldOverrideSpectator((AbstractClientPlayer) (Object) this)) {
            cir.setReturnValue(true);
        }
    }
}
