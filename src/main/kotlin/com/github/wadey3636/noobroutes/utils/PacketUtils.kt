package com.github.wadey3636.noobroutes.utils



import me.noobmodcore.Core.mc
import me.noobmodcore.events.impl.PacketEvent
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object PacketUtils {

    fun sendPacket(packet: Packet<*>?) {
        mc.netHandler.networkManager.sendPacket(packet)
    }

}