package noobroutes.features.floor7.autop3

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S18PacketEntityTeleport
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.features.floor7.autop3.AutoP3.waitingRing
import noobroutes.utils.Scheduler
import noobroutes.utils.skyblock.modMessage
import noobroutes.Core.mc
import noobroutes.events.BossEventDispatcher
import noobroutes.events.impl.Phase
import noobroutes.events.impl.TermOpenEvent
import noobroutes.events.impl.TerminalPhase
import org.lwjgl.input.Mouse

object AutoP3AwaitHandler {

    private var leapedIds = mutableSetOf<Int>()


    private fun leapPlayers(): Int {
        return when {
            BossEventDispatcher.currentBossPhase == Phase.P2 -> 1 //core
            BossEventDispatcher.currentTerminalPhase == TerminalPhase.S3 -> 3 //ee3
            else -> 4
        }
    }

    @SubscribeEvent
    fun awaitingLeap(event: PacketEvent.Receive) {
        if (waitingRing?.leap != true || event.packet !is S18PacketEntityTeleport) return
        val ring = waitingRing ?: return

        val entity  = mc.theWorld.getEntityByID(event.packet.entityId)
        if (entity !is EntityPlayer) return

        val x = event.packet.x shr 5
        val y = event.packet.y shr 5
        val z = event.packet.z shr 5

        if (mc.thePlayer.getDistanceSq(x.toDouble(), y.toDouble(), z.toDouble()) < 5) leapedIds.add(event.packet.entityId)
        if (leapedIds.size == leapPlayers()) {

            if (!ring.inRing()) {
                waitingRing = null
                return
            }
            modMessage("everyone leaped")

            Scheduler.scheduleHighestPostMoveEntityWithHeadingTask {
                ring.maybeDoRing()
                waitingRing = null
            }
        }
    }


    @SubscribeEvent
    fun awaitingTerm(event: TermOpenEvent) {
        waitingRing?.let { ring ->
            if (!ring.term) return

            if (ring.inRing()) {
                Scheduler.scheduleHighestPostMoveEntityWithHeadingTask{
                    ring.maybeDoRing()
                    waitingRing = null
                }
            }
            else waitingRing = null
        }
    }

    @SubscribeEvent
    fun awaitingLeft(event: InputEvent.MouseInputEvent) {
        if (Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()) return

        waitingRing?.let { ring ->
            if (ring.inRing()) {
                Scheduler.scheduleHighestPostMoveEntityWithHeadingTask{
                    ring.maybeDoRing()
                    waitingRing = null
                }
            }
            else waitingRing = null
        }
    }



}