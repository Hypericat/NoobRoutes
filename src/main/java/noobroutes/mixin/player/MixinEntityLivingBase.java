package noobroutes.mixin.player;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.EntityLivingBase;
import noobroutes.events.impl.MoveEntityWithHeadingEvent;
import noobroutes.features.move.QOL;
import noobroutes.utils.skyblock.LocationUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static noobroutes.utils.UtilsKt.postAndCatch;


@Mixin(value = EntityLivingBase.class, priority = 9000)
public abstract class MixinEntityLivingBase {

    @Shadow protected abstract boolean isPlayer();


    @ModifyConstant(
            method = "jump",
            constant = @Constant(floatValue = 0.2F)
    )
    private float noobRoutes$modifySprintJumpBoost(float constant) {
        if (!QOL.INSTANCE.getEnabled() || (!LocationUtils.INSTANCE.isInSkyblock() && !Minecraft.getMinecraft().isSingleplayer())) return constant;
        return QOL.INSTANCE.getSpeed() * Minecraft.getMinecraft().thePlayer.capabilities.getWalkSpeed();
    }

    @ModifyConstant(method = "onLivingUpdate", constant = @Constant(intValue = 10))
    private int noobRoutes$modifyJumpResetTime(int constant) {
        if (!QOL.INSTANCE.getEnabled()) return 10;
        else return QOL.INSTANCE.getJumpDelay();
    }

    @Redirect(method = {"moveEntityWithHeading"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isInLava()Z"))
    private boolean noobRoutes$isInLava(EntityLivingBase entity) {
        if (QOL.INSTANCE.getEnabled() && QOL.INSTANCE.getLavaFix()) return entity.worldObj.isMaterialInBB(entity.getEntityBoundingBox().expand(-1.0E-4D, -1.0E-4D, -1.0E-4D), Material.lava);
        else return entity.isInLava();
    }

    @Inject(
            method = {"moveEntityWithHeading"},
            at = @At("HEAD"),
            require = 0,
            cancellable = true
    )
    private void noobRoutes$onMoveEntityWithHeadingPre$noobroutes(float strafe, float forward, CallbackInfo ci) {
        if ((Object) this instanceof EntityPlayerSP) {
            if (postAndCatch(new MoveEntityWithHeadingEvent.Pre())) ci.cancel();
        }
    }
    @Inject(
            method = {"moveEntityWithHeading"},
            at = @At("TAIL"),
            require = 0,
            cancellable = true
    )
    private void noobRoutes$onMoveEntityWithHeadingPost$noobroutes(float strafe, float forward, CallbackInfo ci) {
        if ((Object) this instanceof EntityPlayerSP) {
            if (postAndCatch(new MoveEntityWithHeadingEvent.Post())) ci.cancel();
        }
    }

}
