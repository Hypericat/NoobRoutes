package com.github.wadey3636.noobroutes.utils

import com.github.wadey3636.noobroutes.utils.ClientUtils.ScheduledTask
import me.defnotstolen.Core.mc
import me.defnotstolen.events.impl.PacketEvent
import me.defnotstolen.utils.skyblock.modMessage
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import kotlin.jvm.Throws

object PacketUtils {
    private val scheduledTasks = mutableListOf<ScheduledTask>()
    fun sendPacket(packet: Packet<*>?) {
        mc.netHandler.networkManager.sendPacket(packet)
    }


    @Throws(IndexOutOfBoundsException::class)
    fun c03ScheduleTask(ticks: Int, callback: () -> Unit) {
        if (ticks < 0) throw IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledTasks.add(ScheduledTask(ticks, callback))
    }



    fun c03ScheduleTask(callback: () -> Unit) {
        scheduledTasks.add(ScheduledTask(0, callback))
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onTick(event: PacketEvent.Send) {
        if (event.packet !is C03PacketPlayer) return
        scheduledTasks.removeAll {
            if (it.ticks <= 0) {
                mc.addScheduledTask { it.callback() }
                true
            } else {
                it.ticks--
                false
            }
        }
    }


}