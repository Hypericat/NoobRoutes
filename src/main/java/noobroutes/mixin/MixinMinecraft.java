package noobroutes.mixin;

import net.minecraft.client.Minecraft;
import noobroutes.events.impl.ClickEvent;
import noobroutes.events.impl.InputEvent;
import noobroutes.features.dungeon.brush.BrushModule;
import noobroutes.features.render.FreeCam;
import noobroutes.utils.SwapManager;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noobroutes.utils.UtilsKt.postAndCatch;


@Mixin(value = {Minecraft.class}, priority = 800)
public class MixinMinecraft {
    @Inject(method = {"runTick"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;dispatchKeypresses()V")})
    public void noobRoutes$keyPresses(CallbackInfo ci) {
        if (Keyboard.getEventKeyState()) postAndCatch(new InputEvent.Keyboard((Keyboard.getEventKey() == 0) ? (Keyboard.getEventCharacter() + 256) : Keyboard.getEventKey()));
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    public void noobRoutes$preRunTick(CallbackInfo ci){
        SwapManager.INSTANCE.onPreRunTick();
    }

    @Inject(method = {"runTick"}, at = {@At(value = "INVOKE", target = "Lorg/lwjgl/input/Mouse;getEventButton()I", remap = false)})
    public void noobRoutes$mouseKeyPresses(CallbackInfo ci) {
        if (Mouse.getEventButtonState()) postAndCatch(new InputEvent.Mouse(Mouse.getEventButton()));
    }

    @Inject(method = "rightClickMouse", at = @At("HEAD"), cancellable = true)
    private void noobRoutes$rightClickMouse(CallbackInfo ci) {
        if (postAndCatch(new ClickEvent.Right()) || postAndCatch(new ClickEvent.All(ClickEvent.ClickType.Right))) ci.cancel();

    }

    @Inject(method = "middleClickMouse", at = @At("HEAD"), cancellable = true)
    private void noobRoutes$middleClickMouse(CallbackInfo ci) {
        if (postAndCatch(new ClickEvent.Middle()) || postAndCatch(new ClickEvent.All(ClickEvent.ClickType.Middle))) ci.cancel();
    }

    @Inject(method = "clickMouse", at = @At("HEAD"), cancellable = true)
    private void noobRoutes$clickMouse(CallbackInfo ci) {
        if (postAndCatch(new ClickEvent.Left()) || postAndCatch(new ClickEvent.All(ClickEvent.ClickType.Left))) ci.cancel();
    }

    @ModifyVariable(method = "sendClickBlockToController", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    public boolean noobRoutes$holdLeftClick(boolean leftClick) {
        if (FreeCam.INSTANCE.getEnabled() || BrushModule.INSTANCE.getEditMode()) {
            return false;
        } else return leftClick;
    }

}