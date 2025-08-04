package noobroutes.features.floor7.autop3

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S18PacketEntityTeleport
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
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
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.Utils.isStart
import noobroutes.utils.addLast
import noobroutes.utils.coerceMax
import noobroutes.utils.getSafe
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.lastSafe
import noobroutes.utils.render.Color
import noobroutes.utils.render.MovementRenderer
import noobroutes.utils.render.RenderUtils
import noobroutes.utils.render.Renderer
import noobroutes.utils.render.getTextHeight
import noobroutes.utils.render.getTextWidth
import noobroutes.utils.render.text
import noobroutes.utils.requirement
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayer
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayerSq
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.util.Stack
@Suppress("Unused")
object AutoP3: Module (
    name = "AutoP3",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "schizo since version 0.0.1"
) {
    private var rings = mutableMapOf<String, MutableList<Ring>>()
    private enum class RingAction{
        Delete,
        Add,
        ChangeActiveBlinkWaypoint,
        AddBlink
    }
    private data class BlinkWaypointState(val state: BlinkWaypoint?, val previousState: BlinkWaypoint?)
    private data class EditRingAction(val action: RingAction, val ring: Ring?, val route: String, val activeBlinkWaypointState: BlinkWaypointState? = null)
    private val recentActionStack = Stack<EditRingAction>()
    private val recentUndoActionStack = Stack<EditRingAction>()

    val route by StringSetting("Route", "", description = "Route to use")
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
    val blinkToggle by BooleanSetting("Blink Toggle", description = "main toggle for blink").withDependency { blinkShit }
    private val maxBlink by NumberSetting("Max Blink", 150, 100, 400, description = "How many packets can be blinked on one instance").withDependency { blinkShit }
    private val balanceHud by HudSetting("Balance Hud", 400f, 400f, 1f, false) {
        if (inF7Boss) text(cancelled.toString(), 1f, 1f, ColorPalette.text, 13f)
        getTextWidth("400", 13f) to getTextHeight("400", 13f)
    }.withDependency { blinkShit }
    private val cancelC05s by BooleanSetting("Cancel C05s", default = false, description = "Allows the cancelling of rotation packets.")
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
    var recordingPacketList = mutableListOf<C03PacketPlayer.C04PacketPlayerPosition>()
        private set

    private var clear = 0

    private fun resetShit(worldChange: Boolean) {
        blinkSetRotation = null
        movementPackets = mutableListOf()
        blinkMovementPacketSkip = false
        activeBlink = null
        activeBlinkWaypoint = null
        recordingPacketList = mutableListOf()

        if (!worldChange) return

        dontCancelNextC03 = false
        cancelled = 0
        toReset = 0
        blinksThisInstance = 0
    }

    @SubscribeEvent
    fun renderRings(event: RenderWorldLastEvent) {
        if (!inF7Boss) return

        rings[route]?.forEachIndexed { i, ring ->
            ring.renderRing(ringColor)

            if (renderIndex) Renderer.drawStringInWorld(i.toString(), ring.coords.add(Vec3(0.0, 0.6, 0.0)), ringColor, depth = true, shadow = false)

            if (ring !is BlinkRing) return@forEachIndexed

            val lastPacket = ring.packets.lastSafe() ?: return@forEachIndexed

            ring.drawCylinderWithRingArgs(Vec3(lastPacket.positionX, lastPacket.positionY, lastPacket.positionZ), Color.RED)

            RenderUtils.drawGradient3DLine(ring.packets.map { Vec3(it.positionX, it.positionY + 0.03, it.positionZ) }, ringColor, Color.RED, 1F, true)
        }

        activeBlinkWaypoint?.renderRing(Color.WHITE)
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onMoveEntityWithHeading(event: MoveEntityWithHeadingEvent.Post) {
        devMessage("MoveEntityWithHeadingEvent")
        if (!inF7Boss || editMode || movementPackets.isNotEmpty()) return

        if (recordingPacketList.isNotEmpty()) {
            val blinkWaypoint = activeBlinkWaypoint ?: return handleMissingWaypoint()

            val c04ToAdd = C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.onGround)
            if (c04ToAdd == recordingPacketList.last()) return

            recordingPacketList.add(c04ToAdd)
            modMessage("recording, ${recordingPacketList.size}")

            if (recordingPacketList.size >= blinkWaypoint.length) {
                addRing(BlinkRing(blinkWaypoint.base, recordingPacketList, mc.thePlayer.motionY))
                recordingPacketList = mutableListOf()
            }
        }

        if (mc.thePlayer.isSneaking) return

        rings[route]?.forEach { ring ->
            if (ring.inRing()) {
                if (ring.triggered) return@forEach
                ring.run()
            }
            else ring.runTriggeredLogic()
        }

        activeBlinkWaypoint?.let {
            if (it.inRing()) {
                if (!it.triggered) startRecording()
                it.triggered = true
            }
            else it.triggered = false
        }
    }

    fun handleMissingWaypoint() {
        modMessage("the blink waypoint was deleted while recording. dont do that shit. bad boy")
        recordingPacketList = mutableListOf()
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
    fun onLeap(event: PacketEvent.Receive) {
        if (!inF7Boss || event.packet !is S08PacketPlayerPosLook || mc.thePlayer?.heldItem?.displayName?.contains("leap", ignoreCase = true) != true) return

        mc.thePlayer.posX = event.packet.x
        mc.thePlayer.posY = event.packet.y
        mc.thePlayer.posZ = event.packet.z

        val blinkRing = rings[route]?.find { it is BlinkRing && it.inRing() } ?: return
        activeBlink = blinkRing as BlinkRing
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



    @SubscribeEvent
    fun onS08(event: S08Event) {
        resetShit(false)
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        resetShit(true)
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        resetShit(true)
    }

    val movementRenderer = MovementRenderer()
    @SubscribeEvent
    fun renderMovement(event: RenderWorldLastEvent) {
        if(!inF7Boss || movementPackets.isEmpty() || !movementMode) return
        val firstPacket = movementPackets.first()
        val beforeFirst = lastMovementedC03




        movementRenderer.renderPlayerAt(
            firstPacket.positionX,
            firstPacket.positionY,
            firstPacket.positionZ,
            beforeFirst.positionX,
            beforeFirst.positionY,
            beforeFirst.positionZ,
            event.partialTicks
        )
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

        activeBlink = null

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

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (mc.thePlayer == null || !event.isStart || movementPackets.isNotEmpty()) return

        activeBlink?.let {
            if (it.inRing()) {
                it.doRing()
            }
            else activeBlink = null
        }
    }

    fun setBlinkRotation(yaw: Float, pitch: Float) {
        blinkSetRotation = Pair(yaw, pitch)
    }

    fun dontCancelNextC03() {
        dontCancelNextC03 = true
    }

    var shouldFreeze = false
        private set



    @SubscribeEvent
    fun noTicks(event: RenderWorldLastEvent) {
        shouldFreeze =
            !(!inF7Boss || !mc.thePlayer.onGround || mc.thePlayer.motionX != 0.0 || mc.thePlayer.motionZ != 0.0 || PlayerUtils.keyBindings.any { it.isKeyDown })
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun cancelC03s(event: PacketEvent.Send) {
        if (!inF7Boss || event.packet !is C03PacketPlayer || movementPackets.isNotEmpty()) return

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

        if (event.packet.isOnGround && !event.packet.isMoving && movementPackets.isEmpty()) {
            event.isCanceled = true
            if (cancelled < 400) cancelled++
            clear = 0
            return
        }
        if (clear > 1) {
            cancelled = 0
            return
        }
        clear++

    }

    fun startRecording() {
        recordingPacketList.add(C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.onGround))
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

    fun setActiveBlinkWaypoint(ring: BlinkWaypoint?) {
        recentActionStack.add(EditRingAction(RingAction.ChangeActiveBlinkWaypoint, null, route, BlinkWaypointState(ring, activeBlinkWaypoint)))
        activeBlinkWaypoint = ring
    }

    fun addRing(ring: Ring){
        rings.getOrPut(route) { mutableListOf() }.add(ring.apply { triggered = true })
        saveRings()
        if (ring is BlinkRing) {
            recentActionStack.add(EditRingAction(
                RingAction.AddBlink,
                ring,
                route,
                BlinkWaypointState(null, activeBlinkWaypoint)
            ))
            activeBlinkWaypoint = null
            modMessage("Added Blink")

        } else {
            recentActionStack.add(EditRingAction(RingAction.Add, ring, route))
            modMessage("Added ${ring.ringName}")
        }
        recentUndoActionStack.clear()
    }

    fun deleteRing(ring: Ring) {
        recentActionStack.add(EditRingAction(RingAction.Delete, ring, route))
        saveRings()
        modMessage("Deleted: ${ring.ringName}")
        rings[route]?.remove(ring)
        recentUndoActionStack.clear()
    }

    fun redo() {
        if (recentUndoActionStack.isEmpty()) return modMessage("Nothing to Redo")
        val ringAction = recentUndoActionStack.pop()
        when (ringAction.action) {
            RingAction.Add -> {
                rings[ringAction.route]?.add(ringAction.ring!!)
                modMessage("Re-Added ${ringAction.ring!!.ringName}")
            }
            RingAction.Delete -> {
                rings[ringAction.route]?.remove(ringAction!!.ring)
                modMessage("Re-Removed ${ringAction.ring!!.ringName}")
            }
            RingAction.ChangeActiveBlinkWaypoint -> {
                activeBlinkWaypoint = ringAction.activeBlinkWaypointState?.state
                if (ringAction.activeBlinkWaypointState?.state == null) {
                    modMessage("Removed Blink Waypoint")
                } else {
                    modMessage("Re-Added Blink Waypoint")
                }
            }
            RingAction.AddBlink -> {
                rings[ringAction.route]?.add(ringAction.ring!!)
                activeBlinkWaypoint = ringAction.activeBlinkWaypointState?.state
                modMessage("Re-Added Blink")
            }
        }
        recentActionStack.add(ringAction)
    }

    fun undo() {
        if (recentActionStack.isEmpty()) return modMessage("Nothing to Undo")
        val ringAction = recentActionStack.pop()
        when (ringAction.action) {
            RingAction.Add -> {
                rings[ringAction.route]?.remove(ringAction!!.ring)
                modMessage("Removed ${ringAction.ring!!.ringName}")
            }
            RingAction.Delete -> {
                rings[ringAction.route]?.add(ringAction.ring!!)
                modMessage("Added back ${ringAction.ring!!.ringName}")
            }
            RingAction.ChangeActiveBlinkWaypoint -> {
                activeBlinkWaypoint = ringAction.activeBlinkWaypointState?.previousState
                if (ringAction.activeBlinkWaypointState?.previousState == null) {
                    modMessage("Removed Blink Waypoint")
                } else {
                    modMessage("Re-Added Blink Waypoint")
                }
            }
            RingAction.AddBlink -> {
                rings[ringAction.route]?.remove(ringAction.ring)
                activeBlinkWaypoint = ringAction.activeBlinkWaypointState?.previousState
            }
        }
        recentUndoActionStack.add(ringAction)
    }

    fun handleDelete(args: Array<out String>) {
        val ringList = rings[route]?.toMutableList()?.apply { activeBlinkWaypoint?.let { add(it) } }
        if (ringList.isNullOrEmpty() ) return modMessage("No Rings to Delete")
        val ring = if (args.requirement(2)) {
            val index = args[1].toIntOrNull() ?: return modMessage("Invalid Index")
            ringList.getSafe(index) ?: return modMessage("Index Out of Bounds")
        } else {
            ringList.minByOrNull { it.coords.distanceToPlayer } ?: return modMessage("No Rings to Delete")
        }
        if (ring is BlinkWaypoint) {
            setActiveBlinkWaypoint(null)
            modMessage("Deleted Blink Waypoint")
            return
        }
        deleteRing(ring)
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