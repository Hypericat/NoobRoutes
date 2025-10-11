package noobroutes.mixin;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetHandlerPlayServer;
import noobroutes.features.floor7.Simulation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;

// This is server side so should only happen in singleplayer anyways
@Mixin(NetHandlerPlayServer.class)
public class NetHandlerPlayServerMixin {
    @Redirect(method = "processPlayer", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
    private boolean onIsPlayerColliding(List instance) {
        // Removes the collision check
        if (Simulation.INSTANCE.allowGhostBLocks()) return true;
        return instance.isEmpty();
    }

    @Redirect(method = "processPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayerMP;isPlayerSleeping()Z", ordinal = 1))
    private boolean onIsPlayerSleeping(EntityPlayerMP instance) {
        // Removes some movement checks in survival
        if (Simulation.INSTANCE.allowGhostBLocks()) return true;
        return instance.isPlayerSleeping();
    }
}
