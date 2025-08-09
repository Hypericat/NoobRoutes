package noobroutes.mixin;


import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import noobroutes.features.render.Esp;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRenderGlobal {
    @Unique
    private boolean override = false;

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/ModelBase;setRotationAngles(FFFFFFLnet/minecraft/entity/Entity;)V", shift = At.Shift.AFTER))
    private<T extends EntityLivingBase> void onPreRenderEntity(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (!Esp.INSTANCE.shouldCancelDepthCheck(entity.getEntityId())) {
            override = false;
            return;
        }
        override = true;
        GlStateManager.disableDepth();
        GlStateManager.disableCull();
        GlStateManager.depthMask(false);
    }

    @Inject(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;disableRescaleNormal()V", shift = At.Shift.AFTER))
    private<T extends EntityLivingBase> void onPostRenderEntity(T entity, double x, double y, double z, float entityYaw, float partialTicks, CallbackInfo ci) {
        if (!override) return;
        GlStateManager.enableDepth();
        GlStateManager.enableCull();
        GlStateManager.depthMask(true);
    }

    @ModifyArg(method = "doRender(Lnet/minecraft/entity/EntityLivingBase;DDDFF)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;depthMask(Z)V"))
    private boolean setDepthMask(boolean flagIn) {
        if (!override) return flagIn;
        return false;
    }


}
