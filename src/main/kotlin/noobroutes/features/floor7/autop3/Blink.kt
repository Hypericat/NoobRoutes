package noobroutes.features.floor7.autop3

import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.Core.mc
import noobroutes.events.BossEventDispatcher
import noobroutes.events.impl.PacketEvent
import noobroutes.features.floor7.autop3.rings.BlinkRing
import noobroutes.features.floor7.autop3.rings.BlinkWaypoint
import noobroutes.utils.AutoP3Utils
import noobroutes.utils.PacketUtils
import noobroutes.utils.SecretGuideIntegration
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.text
import noobroutes.utils.skyblock.modMessage
import kotlin.math.sin


@Suppress("Unused")
object Blink{
    val blinkStarts = mutableListOf<BlinkWaypoint>()
    var cancelled = 0
    var blinksInstance = 0
    var rotate: Float? = null
    private var c03AfterS08 = 0
    var lastBlink = System.currentTimeMillis()
    var lastBlinkRing: Ring? = null
    var movementPackets = mutableListOf<C04PacketPlayerPosition>()
    var endY = 0.0
    var skip = false
    private lateinit var lastWaypoint: BlinkWaypoint
    private var recording = false
    private var recordedPackets = mutableListOf<C04PacketPlayerPosition>()
    private lateinit var lastSentC03: C04PacketPlayerPosition

    fun blinkCommand(args: Array<out String>) {
        if (args.size < 2) return modMessage("need args")
        when (args[1]) {
            "clear" -> {
                blinkStarts.clear()
                modMessage("cleared waypoints. If u want to delete blinks just use /noob delete")
            }
            "more" -> {
                if (args.size < 3) return modMessage("need how many packets to remove")
                val amount = args[2].toIntOrNull() ?: return modMessage("need how many packets to remove")
                blinksInstance -= amount
                modMessage("u now have ${AutoP3.maxBlinks - blinksInstance} packets left on this instance")
            }
            else -> modMessage("not an option")
        }
    }

    @SubscribeEvent
    fun onC03(event: PacketEvent.Send) {
        if (event.packet is C04PacketPlayerPosition) lastSentC03 = event.packet
        else if (event.packet is C06PacketPlayerPosLook) lastSentC03 = C04PacketPlayerPosition(event.packet.positionX, event.packet.positionY, event.packet.positionZ, event.packet.isOnGround)
    }

    @SubscribeEvent
    fun worldLoad(event: WorldEvent.Load) { resetShit() }

    @SubscribeEvent
    fun worldUnLoad(event: WorldEvent.Unload) { resetShit() }

    fun resetShit() {
        blinksInstance = 0
        cancelled = 0
        AutoP3.customBlinkLengthToggle = false
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        if(!BossEventDispatcher.inF7Boss) return
        //val resolution = ScaledResolution(mc)
        //text(cancelled.toString(), resolution.scaledWidth / 2, resolution.scaledHeight / 2.3, Color.WHITE, 13, align = TextAlign.Middle)
        text(
            cancelled.toString(),
            AutoP3.moveHud.x,
            AutoP3.moveHud.y,
            Color.Companion.WHITE,
            13,
            align = TextAlign.Middle
        )
    }
    private var lastMovementedC03: C04PacketPlayerPosition? = null

    @SubscribeEvent
    fun renderMovement(event: RenderWorldLastEvent) {
        if(!BossEventDispatcher.inF7Boss || movementPackets.isEmpty() || lastMovementedC03 == null || !AutoP3.mode) return
        val firstPacket = movementPackets.first()
        val beforeFirst = lastMovementedC03 ?: return
        val xDiff = firstPacket.positionX - beforeFirst.positionX
        val yDiff = firstPacket.positionY - beforeFirst.positionY
        val zDiff = firstPacket.positionZ - beforeFirst.positionZ
        val timeAlong = event.partialTicks
        val xPos = beforeFirst.positionX + xDiff * timeAlong
        val yPos = beforeFirst.positionY + yDiff * timeAlong
        val zPos = beforeFirst.positionZ + zDiff * timeAlong
        Renderer.drawBox(
            AxisAlignedBB(
                xPos + 0.3,
                yPos,
                zPos + 0.3,
                xPos - 0.3,
                yPos + 1.8,
                zPos - 0.3
            ), Color.Companion.GREEN, fillAlpha = 0, outlineWidth = 1.5F)
    }

    private var lastC03 = System.currentTimeMillis()

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onLastC03(event: PacketEvent.Send) {
        if (event.packet is C03PacketPlayer) lastC03 = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun blinkWaypoints(event: RenderWorldLastEvent) {
        if (!BossEventDispatcher.inF7Boss) return
        blinkStarts.forEach {
            Renderer.drawCylinder(it.coords.add(
                Vec3(
                    0.0,
                    0.5 * sin(System.currentTimeMillis().toDouble() / 300) + 0.5,
                    0.0
                )
            ), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.Companion.WHITE, depth = true)
            if (AutoP3.editMode) return
            if (AutoP3Utils.distanceToRingSq(it.coords) < 0.25 && mc.thePlayer.posY == it.coords.yCoord) {
                if (it.triggered) return@forEach
                recordedPackets = mutableListOf<C03PacketPlayer.C04PacketPlayerPosition>()
                startRecording(it)
                it.triggered = true
            }
            else it.triggered = false
        }
    }

    private fun startRecording(waypoint: BlinkWaypoint) {
        modMessage("started recording")
        recordedPackets = mutableListOf<C03PacketPlayer.C04PacketPlayerPosition>()
        recordedPackets.add(lastSentC03)
        lastWaypoint = waypoint
        recording = true

    }

    @SubscribeEvent
    fun movement(event: PacketEvent.Send) {
        if (movementPackets.isEmpty() || event.packet !is C03PacketPlayer) return
        if (skip) {
            skip = false
            return
        }
        event.isCanceled = true
        skip = true
        PacketUtils.sendPacket(movementPackets[0])
        if (!AutoP3.mode) mc.thePlayer.setPosition(movementPackets[0].positionX, movementPackets[0].positionY, movementPackets[0].positionZ)
        if (movementPackets.size == 1) {
            mc.thePlayer.motionY = endY
            mc.thePlayer.setPosition(movementPackets[0].positionX, movementPackets[0].positionY, movementPackets[0].positionZ)
            lastBlink = System.currentTimeMillis()
        }
        lastMovementedC03 = movementPackets.removeFirst()
    }

    @SubscribeEvent
    fun onS08(event: PacketEvent.Receive) {
        if (movementPackets.isEmpty() || event.packet !is S08PacketPlayerPosLook) return
        movementPackets.clear()
    }


    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun recorder(event: PacketEvent.Send) {
        if (!recording || event.packet !is C03PacketPlayer) return
        if (event.isCanceled) return
        modMessage("recording ${recordedPackets.size}")
        if (!event.packet.isMoving) return
        recordedPackets.add(C04PacketPlayerPosition(event.packet.positionX, event.packet.positionY, event.packet.positionZ, event.packet.isOnGround))
        if (recordedPackets.size == getRecordingGoalLength(lastWaypoint)) {
            modMessage("finished recording")
            recording = false
            AutoP3.actuallyAddRing(BlinkRing(
                lastWaypoint.coords,
                lastWaypoint.yaw,
                lastWaypoint.term,
                lastWaypoint.leap,
                lastWaypoint.left,
                lastWaypoint.center,
                lastWaypoint.rotate,
                recordedPackets,
                mc.thePlayer.motionY,
                mc.thePlayer.motionX,
                mc.thePlayer.motionZ,
                lastWaypoint.walk,
                AutoP3Utils.direction
            ))
        }
    }

    fun getRecordingGoalLength(waypoint: BlinkWaypoint): Int {
        return if (AutoP3.customBlinkLengthToggle) AutoP3.customBlinkLength else waypoint.length
    }

    var rotSkip = false

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun canceller(event: PacketEvent.Send) {
        if(!BossEventDispatcher.inF7Boss || event.packet !is C03PacketPlayer) return
        if (rotSkip) {
            rotSkip = false
            return
        }
        if (skip) return
        if (rotate != null) {
            event.isCanceled
            rotate = null
            if (event.packet.isMoving) {
                PacketUtils.sendPacket(C06PacketPlayerPosLook(event.packet.positionX,event.packet.positionY,event.packet.positionZ,rotate!!,0F,event.packet.isOnGround))
            }
            else {
                PacketUtils.sendPacket(C03PacketPlayer.C05PacketPlayerLook(rotate!!, 0F, event.packet.isOnGround))
            }
            if (cancelled > 0) cancelled--
            return
        }
        if (event.packet.isMoving || movementPackets.isNotEmpty() || System.currentTimeMillis() - lastBlink < 100) {
            if (cancelled > 0) cancelled--
            return
        }
        event.isCanceled = true
        if (AutoP3.spedFor == 0) cancelled++
    }
}