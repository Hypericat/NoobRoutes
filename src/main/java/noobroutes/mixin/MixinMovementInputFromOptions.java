package noobroutes.mixin;

import net.minecraft.util.MovementInputFromOptions;
import noobroutes.utils.SpinnySpinManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MovementInputFromOptions.class)
public class MixinMovementInputFromOptions {
    @Inject(method = "updatePlayerMoveState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/settings/KeyBinding;isKeyDown()Z", shift = At.Shift.AFTER, ordinal = 5))
    private void onUpdatePlayerMoveState(CallbackInfo ci) {
        SpinnySpinManager.INSTANCE.adjustMovementInputs((MovementInputFromOptions) (Object) this);
    }
}
