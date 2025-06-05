package noobroutes.mixin.player;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import noobroutes.features.render.FreeCam;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(EntityPlayerSP.class)
public abstract class MixinEntityPlayerSP_Entity extends Entity {
    public MixinEntityPlayerSP_Entity(World world) {
        super(world);
    }

    public void setAngles(float yaw, float pitch) {
        if (FreeCam.INSTANCE.getEnabled()) {
            FreeCam.INSTANCE.setAngles(yaw, pitch);
        } else {
            super.setAngles(yaw, pitch);
        }
    }
}
