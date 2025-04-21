package com.github.wadey3636.noobroutes.utils

import me.modcore.Core.mc
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent

object ClientUtils {
    class ScheduledTask(var ticks: Int, val callback: () -> Unit)
    private val scheduledTasks: MutableList<ScheduledTask> = mutableListOf()



    @Throws(IndexOutOfBoundsException::class)
    fun clientScheduleTask(ticks: Int, callback: () -> Unit) {
        if (ticks < 0) throw IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledTasks.add(ScheduledTask(ticks, callback))
    }



    fun clientScheduleTask(callback: () -> Unit) {
        scheduledTasks.add(ScheduledTask(0, callback))
    }


    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
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