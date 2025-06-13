package noobroutes.mixin.player;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import noobroutes.features.render.FreeCam;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Shadow private int entityId;
    @Unique
    Minecraft mc = Minecraft.getMinecraft();
    @Inject(method = "setAngles", at = @At("HEAD"), cancellable = true)
    public void setAngles(float yaw, float pitch, CallbackInfo ci) {
        if (FreeCam.INSTANCE.getEnabled() && this.entityId == mc.thePlayer.getEntityId()) {
            FreeCam.INSTANCE.setAngles(yaw, pitch);
            ci.cancel();
        }
    }
}
