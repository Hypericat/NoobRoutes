package noobroutes.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraft.network.play.server.S08PacketPlayerPosLook;
import net.minecraft.util.Vec3;
import noobroutes.features.floor7.autop3.AutoP3;
import noobroutes.features.move.QOL;
import noobroutes.mixin.accessors.LastReportedAccessor;
import noobroutes.utils.PacketUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = NetHandlerPlayClient.class, priority = 9000)
public class MixinNetHandlerPlayClient {

    /*@ModifyArg(method = "handlePlayerPosLook", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;sendPacket(Lnet/minecraft/network/Packet;)V"), require = 0)
    public Packet<?> handlePlayerPosLook$noobRoutes(Packet<?> packetIn) {
        PacketUtils.INSTANCE.handleC06ResponsePacket(packetIn);
        return packetIn;
    }*/

    @Inject(method = "handlePlayerPosLook", at = @At("TAIL"))
    public void handlePlayerPosLook(S08PacketPlayerPosLook packetIn, CallbackInfo ci) {
        AutoP3.setLastPosition(Minecraft.getMinecraft().thePlayer.getPositionVector());
    }

    @Redirect(method = "handlePlayerPosLook", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/EntityPlayer;setPositionAndRotation(DDDFF)V"))
    private void fuckRot(EntityPlayer player, double x, double y, double z, float yaw, float pitch) {
        if (!QOL.INSTANCE.getEnabled() || !QOL.INSTANCE.getNoRot()) player.setPositionAndRotation(x, y, z, yaw, pitch);
        else player.setPosition(x, y, z);

        Packet<?> c06 = new C03PacketPlayer.C06PacketPlayerPosLook(x, y, z, yaw, pitch, false);
        PacketUtils.INSTANCE.handleC06ResponsePacket(c06);
        PacketUtils.INSTANCE.sendPacket(c06);

        LastReportedAccessor accessor = (LastReportedAccessor) Minecraft.getMinecraft().thePlayer;
        accessor.setLastReportedPosX(x);
        accessor.setLastReportedPosY(y);
        accessor.setLastReportedPosZ(z);
        accessor.setLastReportedYaw(yaw);
        accessor.setLastReportedPitch(pitch);
        }

    @Redirect(method = "handlePlayerPosLook", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkManager;sendPacket(Lnet/minecraft/network/Packet;)V"))
    private void dontSendPacket(NetworkManager manager, Packet<?> packet) {
        //else dont do anything to not send 2 c06s
    }
}
