package com.github.wadey3636.noobroutes.utils

import me.odinmain.OdinMain.mc
import net.minecraft.network.Packet

object PacketUtils {
    fun sendPacket(packet: Packet<*>?) {
        mc.netHandler.networkManager.sendPacket(packet)
    }
}