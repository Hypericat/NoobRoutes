package noobroutes.mixin.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import noobroutes.events.impl.RenderOverlayNoCaching;
import noobroutes.features.misc.NoDebuff;
import noobroutes.features.render.FreeCam;
import noobroutes.utils.SpinnySpinManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noobroutes.utils.UtilsKt.postAndCatch;

@Mixin(value = EntityRenderer.class)
abstract public class MixinEntityRenderer implements IResourceManagerReloadListener {
    @Inject(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiIngame;renderGameOverlay(F)V", shift = At.Shift.AFTER))
    private void noobRoutes$drawHud(float partialTicks, long nanoTime, CallbackInfo ci) {
        postAndCatch(new RenderOverlayNoCaching(partialTicks));
    }

    @Inject(method = "hurtCameraEffect", at = @At("HEAD"), cancellable = true)
    private void noobRoutes$onHurtCam(float partialTicks, CallbackInfo ci) {
        if (NoDebuff.INSTANCE.getNoHurtCam()) ci.cancel();
    }

    @Inject(method = "renderWorld", at = @At("HEAD"))
    private void noobRoutes$beforeRenderWorld(float partialTicks, long finishTimeNano, CallbackInfo ci){
        FreeCam.INSTANCE.onBeforeRenderWorld(partialTicks);
    }

    @Inject(method = "renderWorld", at = @At("TAIL"))
    private void noobRoutes$afterRenderWorld(float partialTicks, long finishTimeNano, CallbackInfo ci){
        FreeCam.INSTANCE.onAfterRenderWorld();
    }

    @Redirect(method = "updateCameraAndRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;setAngles(FF)V"))
    private void onSetAngles(EntityPlayerSP instance, float yaw, float pitch) {
        if (instance != Minecraft.getMinecraft().thePlayer || !SpinnySpinManager.INSTANCE.handleMouseMovements(yaw, pitch)) instance.setAngles(yaw, pitch);
    }

}
