package noobroutes.mixin.player;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import noobroutes.events.impl.MovePlayerEvent;
import noobroutes.features.move.QOL;
import noobroutes.features.render.FreeCam;
import noobroutes.utils.Utils;
import noobroutes.utils.skyblock.LocationUtils;
import noobroutes.utils.skyblock.PlayerUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static java.lang.Math.atan2;
import static noobroutes.utils.UtilsKt.postAndCatch;

@Mixin(Entity.class)
public abstract class MixinEntity {
    @Shadow private int entityId;

    @Shadow
    protected abstract void resetPositionToBB();

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

    @Inject(method = "moveFlying", at = @At("HEAD"), cancellable = true)
    public void altMovement(float strafe, float forward, float moveSpeed, CallbackInfo ci) {
        if (noobRoutes$mc.thePlayer == null ||
                this.entityId != noobRoutes$mc.thePlayer.getEntityId() ||
                !((Object) this instanceof EntityPlayerSP) || //ide is lying about this
                !QOL.INSTANCE.getEnabled() ||
                !QOL.INSTANCE.getAltMovement() ||
                (!LocationUtils.INSTANCE.isInSkyblock() && !noobRoutes$mc.isSingleplayer()) ||
                noobRoutes$mc.thePlayer.isInLava() ||
                noobRoutes$mc.thePlayer.isInWater() ||
                noobRoutes$mc.thePlayer.isSneaking()
        ) return;

        if (noobRoutes$mc.thePlayer.onGround) {
            int dir = noobRoutes$getDesiredDirection(strafe, forward);

            if (dir == -1) {
                if (QOL.INSTANCE.getInstantStop()) {
                    noobRoutes$mc.thePlayer.motionX = 0.0;
                    noobRoutes$mc.thePlayer.motionZ = 0.0;
                    ci.cancel();
                }
                return;
            }

            float yaw = noobRoutes$mc.thePlayer.rotationYaw;

            double speed = -1.0;

            if (QOL.INSTANCE.getInstantSpeed()) {
                speed = noobRoutes$mc.thePlayer.capabilities.getWalkSpeed() * 2.806;
            }
            if (noobRoutes$mc.thePlayer.motionY == 0.42f && QOL.INSTANCE.getBhopToggle()) { //jumping
                speed = noobRoutes$mc.thePlayer.capabilities.getWalkSpeed() * QOL.INSTANCE.getBhopSpeed();
            }

            if (speed == -1) return;

            noobRoutes$mc.thePlayer.motionX = Utils.INSTANCE.xPart(yaw + dir) * speed;
            noobRoutes$mc.thePlayer.motionZ = Utils.INSTANCE.zPart(yaw + dir) * speed;
            ci.cancel();
        }
    }

    @Unique
    private int noobRoutes$getDesiredDirection(float strafe, float forward) {
        int deltaX = 0;
        if (strafe > 0.5) deltaX = -1;
        else if (strafe < -0.5) deltaX = 1;

        int deltaZ = 0;
        if (forward > 0.5) deltaZ = 1;
        else if (forward < -0.5) deltaZ = -1;

        if (deltaX == 0 && deltaZ == 0) return -1;

        return (int) Math.toDegrees(atan2(deltaX, deltaZ));
    }
}
