package noobroutes.features.floor7.autop3

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S18PacketEntityTeleport
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core.logger
import noobroutes.config.DataManager
import noobroutes.events.BossEventDispatcher
import noobroutes.events.BossEventDispatcher.inF7Boss
import noobroutes.events.impl.*
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.floor7.autop3.rings.BlinkRing
import noobroutes.features.floor7.autop3.rings.BlinkWaypoint
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.*
import noobroutes.ui.ColorPalette
import noobroutes.utils.PacketUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.Utils.isStart
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.render.Color
import noobroutes.utils.render.RenderUtils
import noobroutes.utils.render.Renderer
import noobroutes.utils.render.getTextHeight
import noobroutes.utils.render.getTextWidth
import noobroutes.utils.render.text
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayerSq
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

object AutoP3: Module (
    name = "AutoP3",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "schizo since version 0.0.1"
) {
    private var rings = mutableMapOf<String, MutableList<Ring>>()

    private val route by StringSetting("Route", "", description = "Route to use")
    private val ringColor by ColorSetting("Ring Color", Color.GREEN, false, description = "color of the rings")

    private val editShit by DropdownSetting("Edit Settings", false)
    private val renderIndex by BooleanSetting("Render Index", false, description = "Renders the index of the ring. Useful for creating routes").withDependency { editShit }
    private var editMode by BooleanSetting("Edit Mode", false, description = "Disables ring actions").withDependency { editShit }
    private val editModeKey by KeybindSetting("Toggle Edit Mode", Keyboard.KEY_NONE, "Toggles editmode on press").onPress {
        editMode = !editMode
        modMessage("edit Mode: " + !editMode)
    }.withDependency { editShit }
    val walkBoost by SelectorSetting("Walk Boost", "none", arrayListOf("none", "normal", "big"), description = "how much of a boost to apply walking of edges. Non none values might lagback more").withDependency { editShit }

    private val blinkShit by DropdownSetting("Blink Settings", false)
    private val maxBlink by NumberSetting("Max Blink", 150, 0, 400, description = "How many packets can be blinked on one instance").withDependency { blinkShit }
    private val resetAmount by NumberSetting("Remove Amount", 50, 0, 200, description = "When removing packets from the counter how many to remove").withDependency { blinkShit }
    private val resetInterval by NumberSetting("Remove Interval", 5.0, 5.0, 20.0,0.05, unit = "s" , description = "In what interval to remove packets from the counter").withDependency { blinkShit }
    private val balanceHud by HudSetting("Balance Hud", 400f, 400f, 1f, false) {
        if (inF7Boss) text(cancelled.toString(), 1f, 1f, ColorPalette.text, 13f)
        getTextWidth("400", 13f) to getTextHeight("400", 13f)
    }.withDependency { blinkShit }
    private val movementMode by DualSetting("Movement Mode","Playback", "Silent", false, description = "when unable to blink how the movement should look").withDependency { blinkShit }


    var waitingRing: Ring? = null

    private var leapedIds = mutableSetOf<Int>() //hyper pls forgive me but duplicates would murder me

    private var dontCancelNextC03 = false
    private var blinkSetRotation: Pair<Float, Float>? = null
    var cancelled = 0
    private var toReset = 0
    var blinksThisInstance = 0
    var movementPackets = mutableListOf<C03PacketPlayer.C04PacketPlayerPosition>()
    private lateinit var lastMovementedC03: C03PacketPlayer.C04PacketPlayerPosition
    private var blinkMovementPacketSkip = false
    private var endY = 0.0
    private var activeBlink: BlinkRing? = null

    private var activeBlinkWaypoint: BlinkWaypoint? = null
    private var recordingPacketList = mutableListOf<C03PacketPlayer.C04PacketPlayerPosition>()

    @SubscribeEvent
    fun renderRings(event: RenderWorldLastEvent) {
        if (!inF7Boss) return

        rings[route]?.forEachIndexed { i, ring ->
            ring.renderRing(ringColor)

            if (renderIndex) Renderer.drawStringInWorld(i.toString(), ring.coords.add(Vec3(0.0, 0.6, 0.0)), ringColor, depth = true, shadow = false)

            if (ring !is BlinkRing) return@forEachIndexed

            val lastPacket = ring.packets.last()

            ring.drawCylinderWithRingArgs(Vec3(lastPacket.positionX, lastPacket.positionY, lastPacket.positionZ), ringColor)

            RenderUtils.drawGradient3DLine(ring.packets.map { Vec3(it.positionX, it.positionY + 0.03, it.positionZ) }, Color.GREEN, Color.RED, 1F, true)
        }

        activeBlinkWaypoint?.renderRing(Color.WHITE)
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onMoveEntityWithHeading(event: MoveEntityWithHeadingEvent.Post) {
        if (!inF7Boss || mc.thePlayer.isSneaking || editMode) return

        rings[route]?.forEach { ring ->
            if (ring.inRing()) {
                if (ring.triggered || movementPackets.isNotEmpty()) return@forEach
                ring.run()
            }
            else ring.runTriggeredLogic()
        }

        activeBlink?.let {
            if (it.inRing()) {
                it.doRing()
            }
            else activeBlink = null
        }

        if (activeBlinkWaypoint?.inRing() == true && activeBlinkWaypoint?.triggered == false) {
            startRecording()
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

            Scheduler.schedulePostMoveEntityWithHeadingTask {
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
                Scheduler.schedulePostMoveEntityWithHeadingTask{
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
                Scheduler.schedulePostMoveEntityWithHeadingTask{
                    ring.maybeDoRing()
                    waitingRing = null
                }
            }
            else waitingRing = null
        }
    }

    private fun leapPlayers(): Int {
        return when {
            BossEventDispatcher.currentBossPhase == Phase.P2 -> 1 //core
            BossEventDispatcher.currentTerminalPhase == TerminalPhase.S3 -> 3 //ee3
            else -> 4
        }
    }

    fun getClosestRingToPlayer(): Ring? {
        return rings[route]?.minBy { it.coords.subtract(0.0, mc.thePlayer.eyeHeight.toDouble(), 0.0).distanceToPlayerSq }
    }

    fun addRing(ring: Ring){
        rings.getOrPut(route) { mutableListOf() }.add(ring.apply { triggered = true })
        saveRings()
        modMessage("Added ${ring.ringName}")
    }
    fun deleteRing(ring: Ring){
        rings[route]?.remove(ring)
        saveRings()
        modMessage("Deleted: ${ring.ringName}")
    }


    @SubscribeEvent
    fun renderMovement(event: RenderWorldLastEvent) {
        if(!inF7Boss || movementPackets.isEmpty() || !movementMode) return
        val firstPacket = movementPackets.first()
        val beforeFirst = lastMovementedC03


        val xDiff = firstPacket.positionX - beforeFirst.positionX
        val yDiff = firstPacket.positionY - beforeFirst.positionY
        val zDiff = firstPacket.positionZ - beforeFirst.positionZ

        val xPos = beforeFirst.positionX + xDiff * event.partialTicks
        val yPos = beforeFirst.positionY + yDiff * event.partialTicks
        val zPos = beforeFirst.positionZ + zDiff * event.partialTicks

        //Renderer.renderPlayerAt(xPos, yPos, zPos)
/*
        Renderer.drawBox(
            AxisAlignedBB(
                xPos + 0.3,
                yPos,
                zPos + 0.3,
                xPos - 0.3,
                yPos + 1.8,
                zPos - 0.3
            ), Color.Companion.GREEN, fillAlpha = 0, outlineWidth = 1.5F)

 */
    }

    @SubscribeEvent
    fun movement(event: PacketEvent.Send) {
        if (movementPackets.isEmpty() || event.packet !is C03PacketPlayer) return

        if (blinkMovementPacketSkip) {
            blinkMovementPacketSkip = false
            return
        }
        event.isCanceled = true
        blinkMovementPacketSkip = true
        PacketUtils.sendPacket(movementPackets[0])

        if (!AutoP3.movementMode) mc.thePlayer.setPosition(movementPackets[0].positionX, movementPackets[0].positionY, movementPackets[0].positionZ)
        if (movementPackets.size == 1) {
            mc.thePlayer.motionY = endY
            mc.thePlayer.setPosition(movementPackets[0].positionX, movementPackets[0].positionY, movementPackets[0].positionZ)
        }
        lastMovementedC03 = movementPackets.removeFirst()
    }

    fun setBlinkRotation(yaw: Float, pitch: Float) {
        blinkSetRotation = Pair(yaw, pitch)
    }

    fun dontCancelNextC03() {
        dontCancelNextC03 = true
    }

    @SubscribeEvent
    fun cancelC03s(event: PacketEvent.Send) {
        if (!inF7Boss || event.packet !is C03PacketPlayer) return

        if (dontCancelNextC03) {
            dontCancelNextC03 = false
            return
        }

        if (blinkSetRotation != null) {
            event.isCanceled = true
            dontCancelNextC03 = true

            val yaw = blinkSetRotation?.first ?: return
            val pitch = blinkSetRotation?.second ?: return
            val onGround = event.packet.isOnGround

            blinkSetRotation = null

            if (event.packet.isMoving) {
                val x = event.packet.positionX
                val y = event.packet.positionY
                val z = event.packet.positionZ
                PacketUtils.sendPacket(C03PacketPlayer.C06PacketPlayerPosLook(x, y, z, yaw, pitch, onGround))
            }
            else {
                PacketUtils.sendPacket(
                    C03PacketPlayer.C05PacketPlayerLook(yaw, pitch, onGround)
                )
            }
            return
        }

        if (event.packet.isOnGround && !event.packet.isMoving) {
            event.isCanceled = true
            if (cancelled < 400) cancelled++
        }
    }

    fun startRecording() {

    }

    fun getMaxBlinks(): Int {
        return maxBlink
    }

    fun setEndY(endYVelo: Double) {
        endY = endYVelo
    }

    fun setLastMovementedC03(c04: C03PacketPlayer.C04PacketPlayerPosition) {
        lastMovementedC03 = c04
    }

    fun setActiveBlink(ring: BlinkRing) {
        activeBlink = ring
    }

    fun setActiveBlinkWaypoint(ring: BlinkWaypoint) {
        activeBlinkWaypoint = ring
    }

    @SubscribeEvent
    fun removePackets(event: TickEvent.ClientTickEvent) {
        if (!event.isStart || !BossEventDispatcher.inF7Boss) return
        if (toReset <= 0) {
            cancelled -= resetAmount.coerceAtMost(cancelled)
            toReset = (resetInterval * 20).toInt()
        }
        toReset--
    }

    fun loadRings() {
        rings.clear()
        try {
            val file = DataManager.loadDataFromFileObject("rings")
            for (route in file) {
                val ringsInJson = mutableListOf<Ring>()
                route.value.forEach {
                    val ring = it.asJsonObject
                    val ringType = ring.get("type")?.asString ?: "Unknown"
                    val ringClass = RingType.getTypeFromName(ringType)
                    val instance: Ring = ringClass?.ringClass?.java?.getDeclaredConstructor()?.newInstance() ?: return@forEach
                    instance.base.coords = ring.get("coords").asVec3
                    instance.base.yaw = MathHelper.wrapAngleTo180_float(ring.get("yaw")?.asFloat ?: 0f)
                    instance.base.term = ring.get("term")?.asBoolean == true
                    instance.base.leap = ring.get("leap")?.asBoolean == true
                    instance.base.center = ring.get("center")?.asBoolean == true
                    instance.base.rotate = ring.get("rotate")?.asBoolean == true
                    instance.base.left = ring.get("left")?.asBoolean == true
                    instance.base.diameter = ring.get("diameter")?.asFloat ?: 1f
                    instance.base.height = ring.get("height")?.asFloat ?: 1f
                    instance.loadRingData(ring)
                    ringsInJson.add(instance)
                }
                rings[route.key] = ringsInJson
            }
        } catch (e: Exception) {
            modMessage("Error Loading Rings, Please Send Log to Wadey")
            logger.info(e)
        }
    }
    fun saveRings() {
        try {
            val outObj = JsonObject()
            for ((routeName, rings) in rings) {
                val ringArray = JsonArray().apply {
                    for (ring in rings) {
                        if (ring.type.canSave) add(ring.getAsJsonObject())
                    }
                }
                outObj.add(routeName, ringArray)
            }
            DataManager.saveDataToFile("rings", outObj)
        } catch (e: Exception) {
            modMessage("error saving")
            logger.error("error saving rings", e)
        }
    }
}