package noobroutes.mixin;


import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase extends Entity {
    public MixinEntityLivingBase(World worldIn) {
        super(worldIn);
    }


    /**
     * This makes it so that the block overlay doesn't have a brain aneurysm
     * while using FreeCam or Rotation Visualizer.
     * I don't know why it fixes it, it just does.
     */
    @Inject(method = "getLook", at = @At("HEAD"), cancellable = true)
    private void blockOverlayFix(float partialTicks, CallbackInfoReturnable<Vec3> cir) {
        if ((EntityLivingBase)(Object)this instanceof EntityPlayerSP)
            cir.setReturnValue(noobRoutes$getLook(partialTicks));
    }
    @Unique
    private Vec3 noobRoutes$getLook(float partialTicks){
        if (partialTicks == 1.0F) {
            return this.getVectorForRotation(this.rotationPitch, this.rotationYaw);
        } else {
            float f = this.prevRotationPitch + (this.rotationPitch - this.prevRotationPitch) * partialTicks;
            float f1 = this.prevRotationYaw + (this.rotationYaw - this.prevRotationYaw) * partialTicks;
            return this.getVectorForRotation(f, f1);
        }
    }
}
