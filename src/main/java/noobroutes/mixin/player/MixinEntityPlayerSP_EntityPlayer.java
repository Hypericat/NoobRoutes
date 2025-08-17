package noobroutes.mixin.player;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import noobroutes.events.BossEventDispatcher;
import noobroutes.events.impl.MotionUpdateEvent;
import noobroutes.features.misc.NoDebuff;
import noobroutes.utils.skyblock.PlayerUtils;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static noobroutes.utils.UtilsKt.postAndCatch;

/**
 * Motion Update Event Taken From CGA
 */
@Mixin(value = {EntityPlayerSP.class})
public abstract class MixinEntityPlayerSP_EntityPlayer extends EntityPlayer {
    @Shadow private int positionUpdateTicks;

    @Shadow public abstract boolean isSneaking();

    @Unique
    private double noobRoutes$oldPosX;
    @Unique
    private double noobRoutes$oldPosY;
    @Unique
    private double noobRoutes$oldPosZ;
    @Unique
    private float noobRoutes$oldYaw;
    @Unique
    private float noobRoutes$oldPitch;
    @Unique
    private boolean noobRoutes$oldOnGround;

    public MixinEntityPlayerSP_EntityPlayer(World worldIn, GameProfile gameProfileIn) {
        super(worldIn, gameProfileIn);
    }


    @Inject(method = "onUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;onUpdateWalkingPlayer()V",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true)
    public void onUpdatePre$noobRoutes(CallbackInfo ci) {
        this.noobRoutes$oldPosX = this.posX;
        this.noobRoutes$oldPosY = this.posY;
        this.noobRoutes$oldPosZ = this.posZ;

        this.noobRoutes$oldYaw = this.rotationYaw;
        this.noobRoutes$oldPitch = this.rotationPitch;

        this.noobRoutes$oldOnGround = this.onGround;

        MotionUpdateEvent.Pre motionUpdateEvent = new MotionUpdateEvent.Pre(this.posX, this.posY, this.posZ, this.motionX, this.motionY, this.motionZ, this.rotationYaw, this.rotationPitch, this.onGround);

        if (postAndCatch(motionUpdateEvent)) ci.cancel();

        this.posX = motionUpdateEvent.x;
        this.posY = motionUpdateEvent.y;
        this.posZ = motionUpdateEvent.z;

        this.rotationYaw = motionUpdateEvent.yaw;
        this.rotationPitch = motionUpdateEvent.pitch;

        this.onGround = motionUpdateEvent.onGround;
    }
    /*
    @Inject(method = "isSneaking", at = @At("HEAD"), cancellable = true)
    public void isSneaking$noobRoutes(CallbackInfoReturnable<Boolean> cir){
        if (PlayerUtils.INSTANCE.getServerSideSneak()) {
            cir.setReturnValue(PlayerUtils.INSTANCE.getLastSetSneakState() && !this.sleeping);
        }
    }

     */

    @Redirect(method = "onUpdateWalkingPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isSneaking()Z"))
    public boolean isSneaking$noobRoutes(EntityPlayerSP instance){
        if (PlayerUtils.INSTANCE.getServerSideSneak()) {
            return PlayerUtils.INSTANCE.getLastSetSneakState() && !this.sleeping;
        }
        return instance.isSneaking();
    }

    @Inject(
            method = "onUpdate",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;onUpdateWalkingPlayer()V",
                    shift = At.Shift.AFTER
            ),
            cancellable = true
    )
    public void onUpdatePost$noobRoutes(CallbackInfo ci) {
        this.posX = this.noobRoutes$oldPosX;
        this.posY = this.noobRoutes$oldPosY;
        this.posZ = this.noobRoutes$oldPosZ;

        this.rotationYaw = this.noobRoutes$oldYaw;
        this.rotationPitch = this.noobRoutes$oldPitch;

        this.onGround = this.noobRoutes$oldOnGround;

        MotionUpdateEvent.Post motionUpdateEvent = new MotionUpdateEvent.Post(posX, posY, posZ, motionX, motionY, motionZ, rotationYaw, rotationPitch, onGround);

        if (postAndCatch(motionUpdateEvent)) ci.cancel();

        this.posX = motionUpdateEvent.x;
        this.posY = motionUpdateEvent.y;
        this.posZ = motionUpdateEvent.z;

        this.rotationYaw = motionUpdateEvent.yaw;
        this.rotationPitch = motionUpdateEvent.pitch;

        this.onGround = motionUpdateEvent.onGround;
    }

    @Redirect(method = {"pushOutOfBlocks"}, at = @At(value = "FIELD", target = "Lnet/minecraft/client/entity/EntityPlayerSP;noClip:Z"))
    public boolean shouldPrevent$noobRoutes(EntityPlayerSP instance) {
        return NoDebuff.INSTANCE.getNoPush();
    }

    @Redirect(
            method = "onUpdateWalkingPlayer",
            at = @At(
                    value = "FIELD",
                    target = "Lnet/minecraft/client/entity/EntityPlayerSP;positionUpdateTicks:I",
                    opcode = Opcodes.GETFIELD
            )
    )
    private int alwaysZeroPositionUpdateTicks$noobRoutes(EntityPlayerSP self) {
        if (BossEventDispatcher.INSTANCE.getInF7Boss()) {
            return 0;
        }
        return this.positionUpdateTicks;
    }


}
