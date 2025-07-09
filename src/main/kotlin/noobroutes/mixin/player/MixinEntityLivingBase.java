package noobroutes.mixin.player;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import noobroutes.features.move.QOL;
import noobroutes.utils.skyblock.LocationUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.minecraft.block.material.Material;


@Mixin(EntityLivingBase.class)
public abstract class MixinEntityLivingBase {

    @ModifyConstant(
            method = "jump",
            constant = @Constant(floatValue = 0.2F)
    )
    private float modifySprintJumpBoost(float constant) {
        if (!QOL.INSTANCE.getEnabled() || (!LocationUtils.INSTANCE.isInSkyblock() && !Minecraft.getMinecraft().isSingleplayer())) return constant;
        return QOL.INSTANCE.getSpeed() * Minecraft.getMinecraft().thePlayer.capabilities.getWalkSpeed();
    }

    @ModifyConstant(method = "onLivingUpdate", constant = @Constant(intValue = 10))
    private int modifyJumpResetTime(int constant) {
        if (!QOL.INSTANCE.getEnabled()) return 10;
        else return QOL.INSTANCE.getJumpDelay();
    }

    @Redirect(method = {"moveEntityWithHeading"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityLivingBase;isInLava()Z"))
    private boolean isInLava(EntityLivingBase entity) {
        if (QOL.INSTANCE.getEnabled() && QOL.INSTANCE.getLavaFix()) return entity.worldObj.isMaterialInBB(entity.getEntityBoundingBox().expand(-1.0E-4D, -1.0E-4D, -1.0E-4D), Material.lava);
        else return entity.isInLava();
    }
}
