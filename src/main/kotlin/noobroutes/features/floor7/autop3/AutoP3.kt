package noobroutes.features.floor7.autop3

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core.logger
import noobroutes.config.DataManager
import noobroutes.events.BossEventDispatcher.inF7Boss
import noobroutes.events.impl.*
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.floor7.autop3.rings.BlinkRing
import noobroutes.features.floor7.autop3.rings.BlinkWaypoint
import noobroutes.features.floor7.autop3.rings.HClipRing
import noobroutes.features.floor7.autop3.rings.LavaClipRing
import noobroutes.features.misc.TickControl
import noobroutes.features.render.FreeCam
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.*
import noobroutes.ui.ColorPalette
import noobroutes.ui.editgui.EditGui
import noobroutes.utils.*
import noobroutes.utils.PacketUtils.isResponseToLastS08
import noobroutes.utils.Utils.isStart
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.render.*
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.LowHopUtils
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayer
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayerSq
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
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


    private val RING_HASHCODE = "Ring".hashCode()
    var route by StringSetting("Route", "", description = "Route to use")
    private val sweptCollisionDetection by DualSetting(
        "Detection",
        "Point",
        "Swept",
        default = true,
        description = "Swept Collision Detection uses your previous and current position to check if the path you traversed intersects with a ring as its activation condition. Point based detection checks if your position every tick is inside a ring, as such it will miss rings if you are not precise."
    )

    private val renderSettings by DropdownSetting("Render Settings")
    private val ringColor by ColorSetting("Ring Color", Color.GREEN, false, description = "color of the rings").withDependency { renderSettings }
    private val secondaryRingColor by ColorSetting("Secondary Ring Color", Color.DARK_GRAY, false, description = "The secondary color of the ring for Ring rendering mode").withDependency { renderSettings && renderMode.hashCode() == RING_HASHCODE }
    private val nonSilentRotates by BooleanSetting("Non-Silent look", description = "Makes it so rings with the rotate argument rotate client side.").withDependency { renderSettings }
    private val renderMode by SelectorSetting("Render Mode", "Box", arrayListOf("Box", "BBG", "Simple Ring", "Ring", "Octagon"), description = "Ring render type").withDependency { renderSettings }
    private val renderIndex by BooleanSetting("Render Index", false, description = "Renders the index of the ring. Useful for creating routes").withDependency { renderSettings }
    val showBlinkLine by BooleanSetting("Blink Show Line", description = "if it should render the line showing where the blink goes", default = true).withDependency { renderSettings }
    private val balanceHud by HudSetting("Timer Balance Hud", 400f, 400f, 1f, false) {
        if (inF7Boss) text(cancelled.toString(), 1f, 1f, ColorPalette.textColor, 13f)
        getTextWidth("400", 13f) to getTextHeight("400", 13f)
    }.withDependency { renderSettings }


    private val editShit by DropdownSetting("Edit Settings", false)
    private var editMode by BooleanSetting("Edit Mode", false, description = "Disables ring actions").withDependency { editShit }
    private val editModeKey by KeybindSetting("Toggle Edit Mode", Keyboard.KEY_NONE, "Toggles editmode on press").onPress {
        editMode = !editMode
        modMessage("edit Mode: $editMode")
    }.withDependency { editShit }
    var walkBoost by SelectorSetting("Walk Boost", "none", arrayListOf("None", "Normal", "Large"), description = "how much of a boost to apply walking of edges. Non none values might lagback more").withDependency { editShit }

    private val blinkShit by DropdownSetting("Blink Settings", false)
    val blinkToggle by BooleanSetting("Blink Toggle", description = "main toggle for blink").withDependency { blinkShit }
    private val maxBlink by NumberSetting("Max Blink", 150, 100, 400, description = "How many packets can be blinked on one instance").withDependency { blinkShit }
    val suppressMaxBlink by BooleanSetting("Disable in Singleplayer", description = "Disables the max packets per instance check while in single player").withDependency { blinkShit }
    private val cancelC05Mode by SelectorSetting("Rot Mode", "Always", arrayListOf("Always", "On Blink/Holding Leap", "Never"), description = "when to cancel Rotations").withDependency { blinkShit }
    private val movementMode by DualSetting("Movement Mode","Playback", "Silent", false, description = "when unable to blink how the movement should look").withDependency { blinkShit }
    val dontChargeAll by BooleanSetting("Don't Charge All", description = "Instead of charging all the packets required for a blink only charge some").withDependency { blinkShit }
    val percentageChargeAmount by NumberSetting("Charge Amount", default = 69, min = 30, max = 100, description = "how many percent of the required packets to charge", unit = "%").withDependency { blinkShit &&  dontChargeAll }
    val endBlinkOnKey by BooleanSetting("End On Key", description = "stops recording on keypress instead of having to input the amount").withDependency { blinkShit }
    private val endKey by KeybindSetting("End Keybind", Keyboard.KEY_NONE, "Ends Blink Recording on press").withDependency { blinkShit && endBlinkOnKey }.onPress {
        if (endBlinkOnKey) endBlinkRecording(activeBlinkWaypoint ?: return@onPress)
    }


    var waitingRing: Ring? = null

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
    private var lowHoppedInBlink = false

    private fun resetShit(worldChange: Boolean) {
        blinkSetRotation = null
        movementPackets = mutableListOf()
        blinkMovementPacketSkip = false
        activeBlink = null
        activeBlinkWaypoint = null
        recordingPacketList = mutableListOf()
        lowHoppedInBlink = false

        if (!worldChange) return

        dontCancelNextC03 = false
        cancelled = 0
        toReset = 0
        blinksThisInstance = 0
    }

    @SubscribeEvent
    fun renderRings(event: RenderWorldLastEvent) {
        if (!inF7Boss) return

        rings[route]?.toList()?.forEachIndexed { i, ring ->
            ring.renderRing(ringColor, secondaryRingColor, renderMode)
            if (renderIndex) Renderer.drawStringInWorld(i.toString(), ring.coords.add(Vec3(0.0, 0.6, 0.0)), ringColor, depth = true, shadow = false)
        }

        activeBlinkWaypoint?.renderRing(Color.WHITE, secondaryRingColor, renderMode)
    }


    private fun detectRing(ring: Ring, oldPos: Vec3, newPos: Vec3): Boolean {
        return if (sweptCollisionDetection) {
            ring.intersectedWithRing(oldPos, newPos)
        } else {
            ring.inRing(newPos)
        }
    }

    private var lastPos = Vec3(0.0, 0.0, 0.0)
    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onMoveEntityWithHeading(event: MoveEntityWithHeadingEvent.Post) {
        if (!inF7Boss || editMode || movementPackets.isNotEmpty()) return

        if (mc.thePlayer.isSneaking) {
            lastPos = mc.thePlayer.positionVector
            return
        }
        
        handleRings(mc.thePlayer.positionVector)

        activeBlinkWaypoint?.let { //this needs to be on tick otherwise shit breaks
            if (detectRing(it, lastPos, mc.thePlayer.positionVector)) {
                if (!it.triggered) recording = true
                it.triggered = true
            }
            else it.triggered = false
        }

    }

    fun handleRings(pos: Vec3) {
        val ringList = rings[route] ?: return
        for (ring in ringList) {
            if (detectRing(ring, lastPos, mc.thePlayer.positionVector)) {
                if (ring.triggered) continue
                ring.run()
            } else {
                ring.runTriggeredLogic()
            }
        }
        lastPos = mc.thePlayer.positionVector
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun recordBlink(event: PacketEvent.Send) {
        if (!inF7Boss || editMode || movementPackets.isNotEmpty() || !recording || event.packet !is C03PacketPlayer) return

        if (!event.packet.isMoving) return

        val blinkWaypoint = activeBlinkWaypoint ?: return handleMissingWaypoint()

        val c04ToAdd = C03PacketPlayer.C04PacketPlayerPosition(event.packet.positionX, event.packet.positionY, event.packet.positionZ, event.packet.isOnGround)
        if (LowHopUtils.lowHopThisJump) lowHoppedInBlink = true
        recordingPacketList.add(c04ToAdd)
        modMessage("recording, ${recordingPacketList.size}")

        if (recordingPacketList.size >= blinkWaypoint.length && !endBlinkOnKey) endBlinkRecording(blinkWaypoint)
    }

    private fun endBlinkRecording(blinkWaypoint: BlinkWaypoint) {
        addRing(BlinkRing(blinkWaypoint.base, recordingPacketList, mc.thePlayer.motionY, lowHoppedInBlink))
        recordingPacketList = mutableListOf()
        recording = false
        lowHoppedInBlink = false
    }

    private fun handleMissingWaypoint() {
        modMessage("the blink waypoint was deleted while recording. dont do that shit. bad boy")
        recordingPacketList = mutableListOf()
        recording = false
    }


    @SubscribeEvent
    fun onLeap(event: PacketEvent.Receive) {
        if (!inF7Boss || event.packet !is S08PacketPlayerPosLook) return

        val packetPos = Vec3(event.packet.x, event.packet.y, event.packet.z)

        val blinkRing = rings[route]?.toList()?.find { it is BlinkRing && it.inRing(packetPos) } ?: return
        waitedTicks = 10
        activeBlink = blinkRing as BlinkRing
    }

    fun getClosestRingToPlayer(): Ring? {
        return rings[route]?.toList()?.minBy { it.coords.subtract(0.0, mc.thePlayer.eyeHeight.toDouble(), 0.0).distanceToPlayerSq }
    }

    @JvmStatic
    fun setLastPosition(vec: Vec3){
        lastPos = vec
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
        val movePacket = movementPackets[0]
        PacketUtils.sendPacket(movePacket)

        if (!this.movementMode) setPos(movePacket.positionX, movePacket.positionY, movePacket.positionZ)
        if (movementPackets.size == 1) {
            mc.thePlayer.motionY = endY
            setPos(movePacket.positionX, movePacket.positionY, movePacket.positionZ)
        }
        lastMovementedC03 = movementPackets.removeFirst()
    }

    private fun setPos(x: Double, y: Double, z: Double){
        if (FreeCam.enabled) {
            FreeCam.setPosition(x, y, z)
        }
        mc.thePlayer.setPosition(x, y, z)
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (mc.thePlayer == null || !event.isStart || movementPackets.isNotEmpty()) return

        activeBlink?.let {
            if (it.inRing()) {
                it.doRing()
            } else {
                activeBlink = null
            }
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

    private var resetPacketExceptionState = false

    @SubscribeEvent(priority = EventPriority.LOW)
    fun cancelC03s(event: PacketEvent.Send) {
        if (!inF7Boss || event.packet !is C03PacketPlayer) return

        if (movementPackets.isNotEmpty()) {
            cancelled = 0
            return
        }

        if (dontCancelNextC03) {
            dontCancelNextC03 = false
            cancelled = 0
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

        if (event.packet.isOnGround && !event.packet.isMoving && movementPackets.isEmpty() && (!event.packet.rotating || currentlyCancelRotations())) {
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

    private fun currentlyCancelRotations(): Boolean {
        return when (cancelC05Mode) {
            "Always" -> true
            "On Blink/Holding Leap" -> mc.thePlayer.heldItem?.displayName?.contains("leap", true) == true ||
                    rings[route]?.toList()?.any {it is BlinkRing && it.inRing(mc.thePlayer.positionVector)} == true
            else -> false

        }
    }

    private fun cancelledLogic(){
        cancelled = 0
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
        //nicer to use if it is false while using tick control
        rings.getOrPut(route) { mutableListOf() }.add(ring.apply { triggered = !TickControl.enabled })
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

    fun deleteCorruptedRing(ring: Ring) {
        modMessage("Detected Corrupted Ring: ${ring.ringName}")
        rings[route]?.remove(ring)
        recentUndoActionStack.clear()
        saveRings()
    }

    fun deleteRing(ring: Ring) {
        recentActionStack.add(EditRingAction(RingAction.Delete, ring, route))
        modMessage("Deleted: ${ring.ringName}")
        rings[route]?.remove(ring)
        recentUndoActionStack.clear()
        saveRings()
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
        saveRings()
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
        saveRings()
    }

    private fun getModifyingRingFromArgs(args: Array<out String>): Ring? {
        val ringList = rings[route]?.toMutableList()?.apply { activeBlinkWaypoint?.let { add(it) } }
        if (ringList.isNullOrEmpty() ) {
            modMessage("No Rings to Delete")
            return null
        }
        val arg1 = args.getArg(1, "")

        return if (arg1 != null) {
            val index = arg1.toIntOrNull() ?: run { modMessage("Invalid Index"); return null }
            ringList.getSafe(index) ?: run { modMessage("Index Out of Bounds"); return null }
        } else {
            ringList.minByOrNull { it.coords.distanceToPlayer } ?: run { modMessage("No Rings to Delete"); return null }
        }
    }

    fun handleEdit(args: Array<out String>) {
        val ring = getModifyingRingFromArgs(args) ?: return

        EditGui.openEditGui(ring.getEditGuiBase())
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

                    val awaits = EnumSet.noneOf(RingAwait::class.java)
                    for (await in RingAwait.entries) {
                        if (!ring.has(await.name)) continue
                        awaits.add(await)
                    }

                    instance.ringAwaits = awaits

                    if (ring.has("await")) {
                        try {
                        val await = ring.get("await").asString
                        if (await != "NONE") {

                                instance.ringAwaits.add(RingAwait.getFromName(await))


                        }
                        } catch (e: Exception) {
                            val await = ring.get("await").asString
                            modMessage(await)
                        }
                    }


                    when {
                        ring.get("term")?.asBoolean == true -> {
                            instance.ringAwaits.add(RingAwait.TERM)
                        }

                        ring.get("leap")?.asBoolean == true -> {
                            instance.ringAwaits.add(RingAwait.LEAP)
                        }
                        ring.get("left")?.asBoolean == true -> {
                            instance.ringAwaits.add(RingAwait.LEFT)
                        }
                    }



                    instance.base.center = ring.get("center")?.asBoolean == true
                    instance.base.rotate = ring.get("rotate")?.asBoolean == true
                    instance.base.stopWatch = ring.get("stopwatch")?.asBoolean == true

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
            DataManager.backupFile("rings")
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

        val center = obj.get("center")?.asBoolean == true
        val rotate = obj.get("rotate")?.asBoolean == true
        val stopwatch = obj.get("stopwatch")?.asBoolean == true
        val diameter = obj.get("diameter")?.asFloat ?: 1f
        val height = obj.get("height")?.asFloat ?: 1f
        val walk = obj.get("walk")?.asBoolean == true
        val ringBase = RingBase(coords, yaw, EnumSet.noneOf(RingAwait::class.java), center, rotate,stopwatch, diameter, height)
        when {
            obj.get("term")?.asBoolean == true -> {
                ringBase.await.add(RingAwait.TERM)
            }

            obj.get("leap")?.asBoolean == true -> {
                ringBase.await.add(RingAwait.LEAP)
            }
            obj.get("left")?.asBoolean == true -> {
                ringBase.await.add(RingAwait.LEFT)
            }
        }



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