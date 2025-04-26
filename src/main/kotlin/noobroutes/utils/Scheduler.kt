package noobroutes.utils

import noobroutes.Core.mc
import noobroutes.events.impl.PacketEvent
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraftforge.event.entity.living.LivingEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent

object Scheduler {
    var runTime = 0f

    private val scheduledPreTickTasks = Tasks()
    private val scheduledPostTickTasks = Tasks()
    private val scheduledHighPreTickTasks = Tasks()
    private val scheduledHighPostTickTasks = Tasks()
    private val scheduledLowestPreTickTasks = Tasks()
    private val scheduledLowestPostTickTasks = Tasks()
    private val scheduledPrePlayerTickTasks = Tasks()
    private val scheduledPostPlayerTickTasks = Tasks()
    private val scheduledLowS08Tasks = Tasks()
    private val scheduledPlayerLivingUpdateTasks = Tasks()
    private val scheduledC03Tasks = Tasks()


    @Throws(IndexOutOfBoundsException::class)
    fun schedulePreTickTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit) {
        if (ticks < 0) throw IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledPreTickTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun schedulePostTickTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit) {
        if (ticks < 0) throw IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledPostTickTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }
    @Throws(IndexOutOfBoundsException::class)
    fun scheduleHighPreTickTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit) {
        if (ticks < 0) throw IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledHighPreTickTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }


    @Throws(IndexOutOfBoundsException::class)
    fun scheduleHighPostTickTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit) {
        if (ticks < 0) throw IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledHighPostTickTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun scheduleLowestPreTickTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit) {
        if (ticks < 0) throw IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledLowestPreTickTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun scheduleLowestPostTickTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit) {
        if (ticks < 0) throw IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledLowestPostTickTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun schedulePrePlayerTickTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit) {
        if (ticks < 0) throw IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledPrePlayerTickTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun schedulePostPlayerTickTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit) {
        if (ticks < 0) throw IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledPostPlayerTickTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun scheduleLowS08Task(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit) {
        if (ticks < 0) throw IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledLowS08Tasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun schedulePlayerLivingUpdateTask(ticks: Int = 0, priority: Int = 0, callback: (Any?) -> Unit) {
        if (ticks < 0) throw IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledPlayerLivingUpdateTasks.add(Task({ p -> callback(p) }, ticks, priority))
    }

    @Throws(IndexOutOfBoundsException::class)
    fun scheduleC03Task( ticks: Int = 0, cancel: Boolean = false, priority: Int = 0, callback: (Any?) -> Unit) {
        if (ticks < 0) throw IndexOutOfBoundsException("Scheduled Negative Number")
        scheduledC03Tasks.add(Task({ p -> callback(p) }, ticks, priority, cancel))
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        when (event.phase) {
            TickEvent.Phase.START -> {
                runTime++
                scheduledPreTickTasks.doTasks(event)
            }
            TickEvent.Phase.END -> {
                scheduledPostTickTasks.doTasks(event)
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onLowestTick(event: ClientTickEvent) {
        when (event.phase) {
            TickEvent.Phase.START -> {
                scheduledLowestPreTickTasks.doTasks(event)
            }
            TickEvent.Phase.END -> {
                scheduledLowestPostTickTasks.doTasks(event)
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onHighestTick(event: ClientTickEvent) {
        when (event.phase) {
            TickEvent.Phase.START -> {
                scheduledHighPreTickTasks.doTasks(event)
            }
            TickEvent.Phase.END -> {
                scheduledHighPostTickTasks.doTasks(event)
            }
        }
    }

    @SubscribeEvent
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        if (event.player != mc.thePlayer || event.player == null) return
        when (event.phase) {
            TickEvent.Phase.START -> {
                scheduledPrePlayerTickTasks.doTasks(event)
            }
            TickEvent.Phase.END -> {
                scheduledPostPlayerTickTasks.doTasks(event)
            }
        }
    }

    @SubscribeEvent
    fun lowS08Event(event: PacketEvent.Receive){
        if (event.packet !is S08PacketPlayerPosLook) return
        scheduledLowS08Tasks.doTasks(event)
    }

    @SubscribeEvent
    fun onLivingUpdateEvent(event: LivingEvent.LivingUpdateEvent){
        if (event.entityLiving != mc.thePlayer || event.entityLiving == null) return
        scheduledPlayerLivingUpdateTasks.doTasks(event)
    }

    @SubscribeEvent
    fun onC03PacketEvent(event: PacketEvent.Send){
        if (event.packet !is C03PacketPlayer) return
        if (scheduledC03Tasks.doTasks(event)) event.isCanceled = true
    }


    class Task(val callback: (Any?) -> Unit, var ticks: Int = 0, val priority: Int = 0, val cancel: Boolean = false) : Comparable<Task> {
        var originalIndex: Int = -1
        fun execute(arg: Any?) = callback(arg)

        override fun compareTo(other: Task): Int {
            return when {
                this.priority != other.priority -> other.priority - this.priority
                else -> this.originalIndex - other.originalIndex
            }
        }
    }

    class Tasks {
        private val queue: MutableList<Task> = mutableListOf()

        fun add(task: Task) = queue.add(task)

        fun doTasks(arg: Any? = null): Boolean {
            var cancelled = false
            try {
                queue.forEachIndexed { index, task -> task.originalIndex = index }
                queue.sortWith(compareByDescending<Task> { it.priority }.thenBy { it.originalIndex })

                val after = mutableListOf<Task>()
                val iterator = queue.iterator()
                while (iterator.hasNext()) {
                    val task = iterator.next()
                    iterator.remove()

                    if (task.ticks > 0) {
                        task.ticks--
                        after.add(task)
                    } else {
                        if (task.cancel) cancelled = true
                        task.execute(arg)
                    }
                }
                queue.addAll(after)
            } catch (e: Exception) {
                println("Error while doing tasks: ${e.message}")
            }
            return cancelled
        }
    }



}