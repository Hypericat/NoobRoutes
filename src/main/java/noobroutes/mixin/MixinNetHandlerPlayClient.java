package noobroutes.mixin;

import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;
import noobroutes.utils.PacketUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(value = NetHandlerPlayClient.class, priority = 9000)
public class MixinNetHandlerPlayClient {

    @ModifyArg(method = "handlePlayerPosLook", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;sendPacket(Lnet/minecraft/network/Packet;)V"), require = 1)
    public Packet<?> handlePlayerPosLook$noobRoutes(Packet<?> packetIn) {
        PacketUtils.INSTANCE.handleC06ResponsePacket(packetIn);
        return packetIn;
    }
}
