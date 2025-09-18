package noobroutes.mixin.accessors;

import net.minecraft.client.entity.EntityPlayerSP;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;



@Mixin(value = EntityPlayerSP.class, priority = 800)
public interface LastReportedAccessor {
    @Accessor("lastReportedPosX")
    void setLastReportedPosX(double posX);

    @Accessor("lastReportedPosY")
    void setLastReportedPosY(double posY);

    @Accessor("lastReportedPosZ")
    void setLastReportedPosZ(double posZ);

    @Accessor("lastReportedYaw")
    void setLastReportedYaw(float yaw);

    @Accessor("lastReportedPitch")
    void setLastReportedPitch(float pitch);
}
