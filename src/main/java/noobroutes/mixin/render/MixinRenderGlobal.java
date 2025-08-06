package noobroutes.mixin.render;


import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import noobroutes.features.render.FreeCam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {
    @Inject(
            at = {@At("HEAD")},
            method = {"renderEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V"}
    )
    private void noobRoutes$beforeRenderEntities(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci) {
        FreeCam.INSTANCE.onBeforeRenderEntities();
    }

    @Inject(
            at = {@At("TAIL")},
            method = {"renderEntities(Lnet/minecraft/entity/Entity;Lnet/minecraft/client/renderer/culling/ICamera;F)V"}
    )
    private void noobRoutes$afterRenderEntities(Entity renderViewEntity, ICamera camera, float partialTicks, CallbackInfo ci) {
        FreeCam.INSTANCE.onAfterRenderEntities();
    }
}
