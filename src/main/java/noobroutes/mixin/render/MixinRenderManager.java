package noobroutes.mixin.render;


import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import noobroutes.features.render.FreeCam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RenderManager.class)
public class MixinRenderManager {
    @Inject(
            at = {@At("HEAD")},
            method = {"renderEntitySimple(Lnet/minecraft/entity/Entity;F)Z"}
    )
    private void beforeRenderEntitySimple(Entity entityIn, float partialTicks, CallbackInfoReturnable<Boolean> cir) {
        FreeCam.INSTANCE.onBeforeRenderEntity(entityIn);
    }

    @Inject(
            at = {@At("TAIL")},
            method = {"renderEntitySimple(Lnet/minecraft/entity/Entity;F)Z"}
    )
    private void afterRenderEntitySimple(Entity entityIn, float partialTicks, CallbackInfoReturnable<Boolean> cir) {
        FreeCam.INSTANCE.onAfterRenderEntity(entityIn);
    }

}
