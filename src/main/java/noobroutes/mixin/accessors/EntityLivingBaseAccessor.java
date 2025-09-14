package noobroutes.mixin.accessors;

import net.minecraft.entity.EntityLivingBase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityLivingBase.class)
public interface EntityLivingBaseAccessor {
    @Invoker("getArmSwingAnimationEnd")
    int callGetArmSwingAnimationEnd();

}
