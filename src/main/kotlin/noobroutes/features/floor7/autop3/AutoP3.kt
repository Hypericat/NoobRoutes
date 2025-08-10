package noobroutes.features.floor7.autop3

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S18PacketEntityTeleport
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
import noobroutes.features.floor7.autop3.rings.HClipRing
import noobroutes.features.floor7.autop3.rings.LavaClipRing
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.*
import noobroutes.ui.ColorPalette
import noobroutes.ui.editUI.EditUI
import noobroutes.utils.*
import noobroutes.utils.PacketUtils.isResponseToLastS08
import noobroutes.utils.Utils.isStart
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.render.*
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayer
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayerSq
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.util.*

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
    private val onFrame by BooleanSetting("Check on frame", description = "Checks on frame if you are in a ring. Use if you are lazy.")


    private val editShit by DropdownSetting("Edit Settings", false)
    private val renderIndex by BooleanSetting("Render Index", false, description = "Renders the index of the ring. Useful for creating routes").withDependency { editShit }
    private var editMode by BooleanSetting("Edit Mode", false, description = "Disables ring actions").withDependency { editShit }
    private val editModeKey by KeybindSetting("Toggle Edit Mode", Keyboard.KEY_NONE, "Toggles editmode on press").onPress {
        editMode = !editMode
        modMessage("edit Mode: $editMode")
    }.withDependency { editShit }
    val walkBoost by SelectorSetting("Walk Boost", "none", arrayListOf("none", "normal", "big"), description = "how much of a boost to apply walking of edges. Non none values might lagback more").withDependency { editShit }

    private val blinkShit by DropdownSetting("Blink Settings", false)
    val blinkToggle by BooleanSetting("Blink Toggle", description = "main toggle for blink").withDependency { blinkShit }
    private val maxBlink by NumberSetting("Max Blink", 150, 100, 400, description = "How many packets can be blinked on one instance").withDependency { blinkShit }
    val suppressMaxBlink by BooleanSetting("Disable in Singleplayer", description = "Disables the max packets per instance check while in single player").withDependency { blinkShit }
    private val balanceHud by HudSetting("Balance Hud", 400f, 400f, 1f, false) {
        if (inF7Boss) text(cancelled.toString(), 1f, 1f, ColorPalette.textColor, 13f)
        getTextWidth("400", 13f) to getTextHeight("400", 13f)
    }.withDependency { blinkShit }
    private val cancelC05s by BooleanSetting("Cancel C05s", default = false, description = "Allows the cancelling of rotation packets.").withDependency { blinkShit }
    private val movementMode by DualSetting("Movement Mode","Playback", "Silent", false, description = "when unable to blink how the movement should look").withDependency { blinkShit }
    val x_y0uMode by BooleanSetting("x_y0u Mode", description = "While its faster it also probably flags timer and will lobby you sometimes. (We Jew the Packets -x_y0u)").withDependency { blinkShit }
    val blinkCooldown by NumberSetting("Blink Cooldown", 5, 0, 10, description = "how many ticks to wait after entering a blink ring before allowing blink").withDependency { x_y0uMode && blinkShit }
    private val resetInterval by NumberSetting(name = "clear interval", description = "delete packets periodically", min = 1, max = 300, default = 200, unit = "t").withDependency { x_y0uMode && blinkShit }
    private val resetAmount by NumberSetting(name = "clear amount", description = "delete packets periodically", min = 1, max = 400, default = 50).withDependency { x_y0uMode && blinkShit }
    private val nonSilentRotates by BooleanSetting("Non-Silent look", description = "Makes it so rings with the rotate argument rotate client side.")


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
    var lastBlink = -1L
    var waitedTicks = 0
    inline val isBlinkLimitEnabled get() = !(LocationUtils.isSinglePlayer && suppressMaxBlink)


    private var activeBlinkWaypoint: BlinkWaypoint? = null
    var recordingPacketList = mutableListOf<C03PacketPlayer.C04PacketPlayerPosition>()
        private set
    private var recording = false

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

            ring.drawEnd()

            RenderUtils.drawGradient3DLine(ring.packets.map { Vec3(it.positionX, it.positionY + 0.03, it.positionZ) }, ringColor, Color.RED, 1F, true)
        }

        activeBlinkWaypoint?.renderRing(Color.WHITE)
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onMoveEntityWithHeading(event: MoveEntityWithHeadingEvent.Post) {
        if (!inF7Boss || editMode || movementPackets.isNotEmpty() || mc.thePlayer.isSneaking) return

        if (!onFrame) handleRings(mc.thePlayer.positionVector)

        activeBlinkWaypoint?.let { //this needs to be on tick otherwise shit breaks
            if (it.inRing(mc.thePlayer.positionVector)) {
                if (!it.triggered) recording = true
                it.triggered = true
            }
            else it.triggered = false
        }

    }

    @SubscribeEvent
    fun onFrameRing(event: RenderWorldLastEvent) {
        if (!inF7Boss || editMode || movementPackets.isNotEmpty() || mc.thePlayer.isSneaking || !onFrame) return
        logger.info(PlayerUtils.getEffectiveViewPosition())
        handleRings(PlayerUtils.getEffectiveViewPosition())
    }


    private fun handleRings(pos: Vec3) {
        val ringList = rings[route] ?: return
        for (ring in ringList) {
            if (ring.inRing(pos)) {
                if (ring.triggered) continue
                ring.run()
            } else {
                ring.runTriggeredLogic()
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun recordBlink(event: PacketEvent.Send) {
        if (!inF7Boss || editMode || movementPackets.isNotEmpty() || !recording || event.packet !is C03PacketPlayer) return

        if (!event.packet.isMoving) return //this is a useless check, but I don't want to risk it

        val blinkWaypoint = activeBlinkWaypoint ?: return handleMissingWaypoint()

        val c04ToAdd = C03PacketPlayer.C04PacketPlayerPosition(event.packet.positionX, event.packet.positionY, event.packet.positionZ, event.packet.isOnGround)
        recordingPacketList.add(c04ToAdd)
        modMessage("recording, ${recordingPacketList.size}")

        if (recordingPacketList.size >= blinkWaypoint.length) {
            addRing(BlinkRing(blinkWaypoint.base, recordingPacketList, mc.thePlayer.motionY))
            recordingPacketList = mutableListOf()
            recording = false
        }
    }

    fun handleMissingWaypoint() {
        modMessage("the blink waypoint was deleted while recording. dont do that shit. bad boy")
        recordingPacketList = mutableListOf()
        recording = false
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

    // || mc.thePlayer?.heldItem?.displayName?.contains("leap", ignoreCase = true) != true
    @SubscribeEvent
    fun onLeap(event: PacketEvent.Receive) {
        if (!inF7Boss || event.packet !is S08PacketPlayerPosLook) return

        val packetPos = Vec3(event.packet.x, event.packet.y, event.packet.z)

        val blinkRing = rings[route]?.find { it is BlinkRing && it.inRing(packetPos) } ?: return
        waitedTicks = 10
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
    fun onWorldChange(event: WorldChangeEvent) {
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
        if (resetPacketExceptionState) {
            Scheduler.schedulePreTickTask {
                resetPacketExceptionState = false
                cancelled = 0
            }

        }
    }

    fun setBlinkRotation(yaw: Float, pitch: Float) {
        if (nonSilentRotates) RotationUtils.setAngles(yaw, pitch)
        blinkSetRotation = Pair(yaw, pitch)
    }

    fun dontCancelNextC03() {
        dontCancelNextC03 = true
    }

    @SubscribeEvent
    fun resetter(event: TickEvent.ClientTickEvent) {
        if (!event.isStart || !inF7Boss || !x_y0uMode) return
        if (toReset <= 0) {
            cancelled -= resetAmount.coerceAtMost(cancelled)
            toReset = resetInterval
        }
        toReset--
    }

    private var resetPacketExceptionState = false

    @SubscribeEvent(priority = EventPriority.LOW)
    fun cancelC03s(event: PacketEvent.Send) {
        if (!inF7Boss || event.packet !is C03PacketPlayer) return

        if (movementPackets.isNotEmpty()) {
            if (!x_y0uMode) cancelled = 0
            return
        }

        if (dontCancelNextC03) {
            dontCancelNextC03 = false
            if (!x_y0uMode) cancelled = 0
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

        if (event.packet.isOnGround && !event.packet.isMoving && movementPackets.isEmpty() && (!event.packet.rotating || cancelC05s)) {
            event.isCanceled = true
            if (cancelled < 400) cancelled++
            return
        } else {

            if (event.packet is C03PacketPlayer.C06PacketPlayerPosLook && event.packet.isResponseToLastS08()) {
                resetPacketExceptionState = true
                return
            }
            if (resetPacketExceptionState) return
            cancelledLogic()
        }
    }

    private fun cancelledLogic(){
        if (x_y0uMode) cancelled.coerceAtMost(200)
        else cancelled = 0
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

    fun toggleEditMode(){
        editMode = !editMode
        if (editMode) {
            modMessage("AutoP3 Edit Mode §aEnabled")
        } else {
            modMessage("AutoP3 Edit Mode §cDisabled")
        }
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
    private fun getModifyingRingFromArgs(args: Array<out String>): Ring? {
        val ringList = rings[route]?.toMutableList()?.apply { activeBlinkWaypoint?.let { add(it) } }
        if (ringList.isNullOrEmpty() ) {
            modMessage("No Rings to Delete")
            return null
        }
        return if (args.requirement(2)) {
            val index = args[1].toIntOrNull() ?: run { modMessage("Invalid Index"); return null }
            ringList.getSafe(index) ?: run { modMessage("Index Out of Bounds"); return null }
        } else {
            ringList.minByOrNull { it.coords.distanceToPlayer } ?: run { modMessage("No Rings to Delete"); return null }
        }
    }

    fun handleEdit(args: Array<out String>) {
        EditUI.openUI(getModifyingRingFromArgs(args) ?: return)
    }

    fun handleDelete(args: Array<out String>) {
        val ring = getModifyingRingFromArgs(args) ?: return
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
                    if (ringClass == null) {
                        loadOldRing(ring, ringsInJson)
                        return@forEach
                    }
                    val instance: Ring = ringClass.ringClass.java.getDeclaredConstructor().newInstance() ?: return@forEach
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

    fun loadOldRing(obj: JsonObject, ringsInJson: MutableList<Ring>){
        val ringType = obj.get("type")?.asString ?: return
        val coords = obj.get("coords").asVec3
        val yaw = MathHelper.wrapAngleTo180_float(obj.get("yaw")?.asFloat ?: 0f)
        val term = obj.get("term")?.asBoolean == true
        val leap = obj.get("leap")?.asBoolean == true
        val center = obj.get("center")?.asBoolean == true
        val rotate = obj.get("rotate")?.asBoolean == true
        val left = obj.get("left")?.asBoolean == true
        val diameter = obj.get("diameter")?.asFloat ?: 1f
        val height = obj.get("height")?.asFloat ?: 1f
        val walk = obj.get("walk")?.asBoolean == true
        val ringBase = RingBase(coords, yaw, term, leap, left, center, rotate, diameter, height)
        when (ringType) {
            "Insta" -> {
                ringsInJson.add(
                    HClipRing(ringBase, walk, true)
                )
            }
            "LavaClip" -> {
                val length = obj.get("length")?.asDouble ?: return
                ringsInJson.add(LavaClipRing(ringBase, length))
            }
        }
    }
}