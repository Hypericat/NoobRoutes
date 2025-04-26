package noobroutes.utils



import noobroutes.Core.mc
import net.minecraft.network.Packet

object PacketUtils {

    fun sendPacket(packet: Packet<*>?) {
        mc.netHandler.networkManager.sendPacket(packet)
    }

}