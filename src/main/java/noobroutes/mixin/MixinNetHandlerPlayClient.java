package noobroutes.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.Vec3;
import noobroutes.features.floor7.autop3.AutoP3;
import noobroutes.utils.PacketUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetHandlerPlayClient.class, priority = 9000)
public class MixinNetHandlerPlayClient {

    @ModifyArg(method = "handlePlayerPosLook", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;sendPacket(Lnet/minecraft/network/Packet;)V"), require = 0)
    public Packet<?> handlePlayerPosLook$noobRoutes(Packet<?> packetIn) {
        PacketUtils.INSTANCE.handleC06ResponsePacket(packetIn);
        return packetIn;
    }

    @Inject(method = "handlePlayerPosLook", at = @At("TAIL"))
    public void handlePlayerPosLook(S08PacketPlayerPosLook packetIn, CallbackInfo ci) {
        AutoP3.setLastPosition(Minecraft.getMinecraft().thePlayer.getPositionVector());
    }
}
