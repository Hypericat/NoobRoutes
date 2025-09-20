package noobroutes.features.floor7.autop3

import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S18PacketEntityTeleport
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.Core.mc
import noobroutes.events.BossEventDispatcher
import noobroutes.events.impl.BossEvent
import noobroutes.events.impl.ChatPacketEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.Phase
import noobroutes.events.impl.TermOpenEvent
import noobroutes.events.impl.TerminalPhase
import noobroutes.features.floor7.autop3.AutoP3.waitingRing
import noobroutes.utils.Scheduler
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.modMessage
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

    private fun doRingSafe(ring: Ring): Boolean {
        waitingRing = null
        if (!ring.inRing(mc.thePlayer.positionVector)) return false
        Scheduler.schedulePreTickTask { ring.maybeDoRing() }
        return true
    }

    @SubscribeEvent
    fun awaitingLeap(event: PacketEvent.Receive) {
        if (waitingRing?.await != RingAwait.LEAP || event.packet !is S18PacketEntityTeleport) return
        val ring = waitingRing ?: return

        val entity  = mc.theWorld.getEntityByID(event.packet.entityId)
        if (entity !is EntityPlayer) return

        val x = event.packet.x shr 5
        val y = event.packet.y shr 5
        val z = event.packet.z shr 5

        if (mc.thePlayer.getDistanceSq(x.toDouble(), y.toDouble(), z.toDouble()) < 5) leapedIds.add(event.packet.entityId)
        if (leapedIds.size == leapPlayers()) {

            modMessage("everyone leaped")
            doRingSafe(ring)
        }
    }


    @SubscribeEvent
    fun awaitingTerm(event: TermOpenEvent) {
        val ring = waitingRing ?: return

        if (ring.await != RingAwait.TERM) return
        doRingSafe(ring)
    }

    @SubscribeEvent
    fun awaitingLeft(event: MouseEvent) {
        val ring = waitingRing ?: return

        if (Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()) return

        if (doRingSafe(ring)) {
            event.isCanceled = true
            PlayerUtils.swing()
        }
    }

    private val devRegex = Regex("^(\\w+) completed a device! \\(\\d/\\d\\)")
    private val leverRegex = Regex("^(\\w+) completed a lever! \\(\\d/\\d\\)")

    @SubscribeEvent
    fun onChat(event: ChatPacketEvent) {
        val ring = waitingRing ?: return

        val match = when (ring.await) {
            RingAwait.COMPLETE_DEV -> {devRegex.find(event.message)}
            RingAwait.COMPLETE_LEVER -> {leverRegex.find(event.message)}
            else -> return
        } ?: return

        if (mc.thePlayer.name != match.groups[1].toString()) return

        doRingSafe(ring)
    }

    @SubscribeEvent
    fun onTerminalPhaseChange(event: BossEvent.TerminalPhaseChange) {
        val ring = waitingRing ?: return
        if (!ring.await.matchesTerminalPhase(event.phase)) return
        doRingSafe(ring)

    }

    @SubscribeEvent
    fun onBossPhaseChange(event: BossEvent.PhaseChange) {
        val ring = waitingRing ?: return
        if (!ring.await.matchesBossPhase(event.phase)) return
        doRingSafe(ring)

    }



}