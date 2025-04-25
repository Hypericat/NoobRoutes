package com.github.wadey3636.noobroutes.utils



import me.noobmodcore.Core.mc
import net.minecraft.network.Packet

object PacketUtils {

    fun sendPacket(packet: Packet<*>?) {
        mc.netHandler.networkManager.sendPacket(packet)
    }

}