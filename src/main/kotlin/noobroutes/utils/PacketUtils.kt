package noobroutes.utils



import net.minecraft.network.Packet
import noobroutes.Core.mc

object PacketUtils {
    fun sendPacket(packet: Packet<*>?) {
        mc.netHandler.networkManager.sendPacket(packet)
    }
}