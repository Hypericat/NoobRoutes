package noobroutes.mixin.player;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import noobroutes.features.move.QOL;
import noobroutes.utils.skyblock.LocationUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;


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
}
