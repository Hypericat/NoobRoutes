package com.github.wadey3636.noobroutes.utils



import me.noobmodcore.Core.mc
import me.noobmodcore.events.impl.PacketEvent
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object PacketUtils {
    class ScheduledTaskC03(var ticks: Int, val callback: () -> Unit, val cancel: Boolean)
    private val scheduledTasks = mutableListOf<ScheduledTaskC03>()
    fun sendPacket(packet: Packet<*>?) {
        mc.netHandler.networkManager.sendPacket(packet)
    }

    @Throws(IndexOutOfBoundsException::class)
    fun c03ScheduleTask(ticks: Int, cancel: Boolean = false, callback: () -> Unit) {
        if (ticks < 0) throw IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledTasks.add(ScheduledTaskC03(ticks, callback, cancel))
    }



    fun c03ScheduleTask(cancel: Boolean = false, callback: () -> Unit) {
        scheduledTasks.add(ScheduledTaskC03(0, callback, cancel))
    }


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onPacket(event: PacketEvent.Send) {
        if (event.packet !is C03PacketPlayer) return
        val toRun = mutableListOf<() -> Unit>()
        scheduledTasks.removeAll {
            if (it.ticks <= 0) {
                if (it.cancel) event.isCanceled = true
                toRun.add(it.callback)
                true
            } else {
                it.ticks--
                false
            }
        }
        toRun.forEach {
            mc.addScheduledTask { it() }
        }


    }


}