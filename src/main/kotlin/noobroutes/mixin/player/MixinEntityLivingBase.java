package noobroutes.mixin.player;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.EntityLivingBase;
import noobroutes.features.move.BHop;
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
        if (!BHop.INSTANCE.getEnabled() || (!LocationUtils.INSTANCE.isInSkyblock() && !Minecraft.getMinecraft().isSingleplayer())) return constant;
        return BHop.INSTANCE.getSpeed() * Minecraft.getMinecraft().thePlayer.capabilities.getWalkSpeed();
    }
}
