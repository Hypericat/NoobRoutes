package noobroutes.features
import noobroutes.Core.mc
import noobroutes.events.impl.PacketEvent
import noobroutes.features.floor7.AutoP3
import noobroutes.features.floor7.AutoP3.blink
import noobroutes.features.floor7.AutoP3.customBlinkLength
import noobroutes.features.floor7.AutoP3.customBlinkLengthToggle
import noobroutes.features.floor7.AutoP3.inBoss
import noobroutes.features.floor7.AutoP3.maxBlinks
import noobroutes.features.floor7.AutoP3.mode
import noobroutes.features.floor7.Ring
import noobroutes.features.floor7.RingTypes
import noobroutes.utils.AutoP3Utils
import noobroutes.utils.PacketUtils
import noobroutes.utils.SecretGuideIntegration
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.text
import noobroutes.utils.skyblock.modMessage
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.*
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.sin

object Blink{

    data class BlinkWaypoints (val coords: Vec3 = mc.thePlayer.positionVector, val length: Int, var active: Boolean = false)
    private val blinkStarts = mutableListOf<BlinkWaypoints>()

    var cancelled = 0

    private var blinksInstance = 0

    var rotate: Float? = null
    private var awaitingRotation = false

    private var c03AfterS08 = 0

    var lastBlink = System.currentTimeMillis()
    var lastBlinkRing: Ring? = null

    var movementPackets = mutableListOf<C04PacketPlayerPosition>()
    private var endY = 0.0
    var skip = false
    private lateinit var lastWaypoint: BlinkWaypoints

    private var recording = false
    private var recordedPackets = mutableListOf<C04PacketPlayerPosition>()
    private lateinit var lastSentC03: C04PacketPlayerPosition

    fun blinkCommand(args: Array<out String>) {
        if (args.size < 2) return modMessage("need args")
        when (args[1]) {
            "add" -> {
                try {
                    val length = args[2].toInt()
                    blinkStarts.add(BlinkWaypoints(length = length))
                } catch (e: Exception) {
                    modMessage("need length arg")
                }
            }
            "clear" -> {
                blinkStarts.clear()
                modMessage("cleared waypoints. If u want to delete blinks just use /noob delete")
            }
            "more" -> {
                try {
                    val amount = args[2].toInt()
                    blinksInstance -= amount
                    modMessage("u now have ${maxBlinks - blinksInstance} packets left on this instance")
                } catch (e: Exception) {
                    modMessage("need length amount to remove")
                }
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
    fun worldLoad(event: WorldEvent.Load) {
        blinksInstance = 0
        cancelled = 0
        SecretGuideIntegration.setSecretGuideAura(true)
        inBoss = false
        customBlinkLengthToggle = false
    }

    @SubscribeEvent
    fun worldUnLoad(event: WorldEvent.Unload) {
        blinksInstance = 0
        cancelled = 0
        SecretGuideIntegration.setSecretGuideAura(true)
        inBoss = false
        customBlinkLengthToggle = false
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return
        if(!inBoss) return
        //val resolution = ScaledResolution(mc)
        //text(cancelled.toString(), resolution.scaledWidth / 2, resolution.scaledHeight / 2.3, Color.WHITE, 13, align = TextAlign.Middle)
        text(cancelled.toString(), AutoP3.moveHud.x, AutoP3.moveHud.y, Color.WHITE, 13, align = TextAlign.Middle)
    }
    private var lastMovementedC03: C04PacketPlayerPosition? = null
    @SubscribeEvent
    fun renderMovement(event:RenderWorldLastEvent) {
        if(!inBoss) return
        if (movementPackets.isEmpty() || lastMovementedC03 == null) return
        if (!mode) return
        val firstPacket = movementPackets.first()
        val beforeFirst = lastMovementedC03 ?: return
        val xDiff = firstPacket.positionX - beforeFirst.positionX
        val yDiff = firstPacket.positionY - beforeFirst.positionY
        val zDiff = firstPacket.positionZ - beforeFirst.positionZ
        val timeAlong = (System.currentTimeMillis() - lastC03) / 50.0
        val xPos = beforeFirst.positionX + xDiff * timeAlong
        val yPos = beforeFirst.positionY + yDiff * timeAlong
        val zPos = beforeFirst.positionZ + zDiff * timeAlong
        Renderer.drawBox(AxisAlignedBB(xPos+0.3, yPos, zPos+0.3, xPos-0.3, yPos+1.8, zPos-0.3), Color.GREEN, fillAlpha = 0, outlineWidth = 1.5F)
    }

    private var lastC03 = System.currentTimeMillis()
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onLastC03(event: PacketEvent.Send) {
        if (event.packet is C03PacketPlayer) lastC03 = System.currentTimeMillis()
    }

    @SubscribeEvent
    fun blinkWaypoints(event: RenderWorldLastEvent) {
        if(!inBoss) return
        blinkStarts.forEach {
            Renderer.drawCylinder(it.coords.add(Vec3(0.0, 0.5 * sin(System.currentTimeMillis().toDouble()/300) + 0.5 , 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.WHITE, depth = true)
            if (AutoP3.editMode) return
            if (AutoP3Utils.distanceToRingSq(it.coords) < 0.25 && mc.thePlayer.posY == it.coords.yCoord && it.active) {
                recordedPackets = mutableListOf<C04PacketPlayerPosition>()
                startRecording(it)
                it.active = false
            }
            else if(AutoP3Utils.distanceToRingSq(it.coords) > 0.25 || mc.thePlayer.posY != it.coords.yCoord) it.active = true
        }
    }

    private fun startRecording(waypoint: BlinkWaypoints) {
        modMessage("started recording")
        recordedPackets = mutableListOf<C04PacketPlayerPosition>()
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
        if (!mode) mc.thePlayer.setPosition(movementPackets[0].positionX, movementPackets[0].positionY, movementPackets[0].positionZ)
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

    fun doBlink(ring: Ring) {
        if (movementPackets.isNotEmpty()) return



        if (System.currentTimeMillis() - lastBlink >= 500 && (blinksInstance + ring.blinkPackets.size > maxBlinks || !blink)) {
            modMessage("movementing")
            movementPackets = ring.blinkPackets.toMutableList()
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionY = 0.0
            endY = ring.endY
            lastBlink = System.currentTimeMillis()
            lastBlinkRing = null
            return
        }

        if (cancelled < ring.blinkPackets.size || blinksInstance + ring.blinkPackets.size > maxBlinks || System.currentTimeMillis() - lastBlink < 500) {
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
            return
        }
        //modMessage("blinking with ${ring.blinkPackets.size} of those suckers")
        blinksInstance += ring.blinkPackets.size
        lastBlink = System.currentTimeMillis()
        lastBlinkRing = ring
        ring.blinkPackets.forEach { PacketUtils.sendPacket(it) }
        val lastPacket = ring.blinkPackets.size - 1
        mc.thePlayer.setPosition(ring.blinkPackets[lastPacket].positionX, ring.blinkPackets[lastPacket].positionY, ring.blinkPackets[lastPacket].positionZ)
        mc.thePlayer.setVelocity(0.0, ring.endY, 0.0)
        //modMessage("there are $cancelled hot C04s wanting to message u but only ${maxBlinks - blinksInstance} on this instance")
        modMessage("§c§l$cancelled§r§f c04s available, used §c${ring.blinkPackets.size}§f,  §7(${maxBlinks - blinksInstance} left on this instance)")
    }

    @SubscribeEvent
    fun recorder(event: PacketEvent.Send) {
        if (!recording || event.packet !is C03PacketPlayer) return
        if (event.isCanceled) return
        modMessage("recording ${recordedPackets.size}")
        if (event.packet is C04PacketPlayerPosition) {
            if (recordedPackets.last() == C04PacketPlayerPosition(event.packet.positionX, event.packet.positionY, event.packet.positionZ, event.packet.isOnGround)) return
            recordedPackets.add(event.packet)
        }
        else if (event.packet is C06PacketPlayerPosLook) {
            if (recordedPackets.last() == C04PacketPlayerPosition(event.packet.positionX, event.packet.positionY, event.packet.positionZ, event.packet.isOnGround)) return
            recordedPackets.add(C04PacketPlayerPosition(event.packet.positionX, event.packet.positionY, event.packet.positionZ, event.packet.isOnGround))
        }
        if (recordedPackets.size == getRecordingGoalLength(lastWaypoint)) {
            modMessage("finished recording")
            recording = false
            AutoP3.actuallyAddRing(Ring(RingTypes.BLINK, coords = lastWaypoint.coords,  blinkPackets = recordedPackets, endY = mc.thePlayer.motionY))
        }
    }

    fun getRecordingGoalLength(waypoint: BlinkWaypoints): Int {
        return if (customBlinkLengthToggle) customBlinkLength else waypoint.length
    }

    @SubscribeEvent
    fun s08(event: PacketEvent.Receive) {
        if (!inBoss) return
        if (event.packet is S08PacketPlayerPosLook) c03AfterS08 = 2
    }

    var rotSkip = false

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun canceller(event: PacketEvent.Send) {
        if(!inBoss) return
        if (event.packet !is C03PacketPlayer) return
        if (rotSkip) {
            rotSkip = false
            return
        }

        if (skip) return
        if (awaitingRotation) {
            awaitingRotation = false
            return
        }
        if (rotate != null) {
            if (event.packet is C04PacketPlayerPosition || event.packet is C06PacketPlayerPosLook) {
                event.isCanceled = true
                awaitingRotation = true
                PacketUtils.sendPacket(C06PacketPlayerPosLook(event.packet.positionX, event.packet.positionY, event.packet.positionZ, rotate!!, 0F, event.packet.isOnGround))
            }
            else {
                event.isCanceled = true
                awaitingRotation = true
                PacketUtils.sendPacket(C05PacketPlayerLook(rotate!!, 0F, event.packet.isOnGround))
            }
            rotate = null
            if(cancelled > 0) cancelled--
            return
        }
        if (c03AfterS08 > 0) {
            c03AfterS08--
            if (cancelled > 0) cancelled--
            return
        }
        if (mc.thePlayer.posX != mc.thePlayer.lastTickPosX ||
            mc.thePlayer.posY != mc.thePlayer.lastTickPosY ||
            mc.thePlayer.posZ != mc.thePlayer.lastTickPosZ ||
            !mc.thePlayer.onGround || mc.thePlayer.motionX != 0.0 ||
            mc.thePlayer.motionZ != 0.0 ||
            movementPackets.isNotEmpty() ||
            (mc.thePlayer.getDistanceSq(63.5, 127.0, 35.5) < 1.25 && event.packet is C05PacketPlayerLook) ||
            System.currentTimeMillis() - lastBlink < 1 //listen if it works it works
            //gay - wadey
        ) {
            if (cancelled > 0) cancelled--
            return
        }
        event.isCanceled = true
        if (AutoP3.spedFor == 0) cancelled++
    }
}