package noobroutes.mixin.player;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import noobroutes.events.impl.MovePlayerEvent;
import noobroutes.features.render.FreeCam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noobroutes.utils.UtilsKt.postAndCatch;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Shadow private int entityId;
    @Unique
    Minecraft noobRoutes$mc = Minecraft.getMinecraft();
    @Inject(method = "setAngles", at = @At("HEAD"), cancellable = true)
    public void noobRoutes$setAngles(float yaw, float pitch, CallbackInfo ci) {
        if (FreeCam.INSTANCE.getEnabled() && this.entityId == noobRoutes$mc.thePlayer.getEntityId()) {
            FreeCam.INSTANCE.setAngles(yaw, pitch);
            ci.cancel();
        }
    }

    @Inject(method = "moveEntity", at = @At("HEAD"), cancellable = true)
    public void noobRoutes$moveEntity(double x, double y, double z, CallbackInfo ci){
        if (noobRoutes$mc.thePlayer == null || this.entityId != noobRoutes$mc.thePlayer.getEntityId()) return;
        if (postAndCatch(new MovePlayerEvent())) ci.cancel();
    }
}
