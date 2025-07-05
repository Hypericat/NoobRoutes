package noobroutes.features.floor7.autop3

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.server.S18PacketEntityTeleport
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.Core.logger
import noobroutes.config.DataManager
import noobroutes.events.BossEventDispatcher
import noobroutes.events.BossEventDispatcher.inF7Boss
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.Phase
import noobroutes.events.impl.TermOpenEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.floor7.autop3.Blink.blinkStarts
import noobroutes.features.floor7.autop3.rings.*
import noobroutes.features.misc.SexAura
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.*
import noobroutes.ui.editUI.EditUI
import noobroutes.ui.hud.HudElement
import noobroutes.utils.*
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.render.Color
import noobroutes.utils.render.RenderUtils
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import kotlin.math.absoluteValue


@Suppress("Unused")
object AutoP3: Module (
    name = "AutoP3",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "AutoP3"
) {
    private val route by StringSetting("Route", "", description = "Route to use")
    val editMode by BooleanSetting("Edit Mode", false, description = "Disables ring actions")
    val depth by BooleanSetting("Depth Check", true, description = "Makes rings render through walls")
    private val renderIndex by BooleanSetting("Render Index", false, description = "Renders the index of the ring. Useful for creating routes")
    var noRotate by BooleanSetting("no rotate", false, description = "forces the player to be unable to change where they look in boss. Pretty much only way to gurantee working motion rings")
    private val noRotateKey by KeybindSetting("toggle no rotate", Keyboard.KEY_NONE, "toggles no rotate setting").onPress {
        noRotate = !noRotate
        modMessage("can rotate: " + !noRotate)
    }
    val cgyMode by BooleanSetting("cgy Mode", false, description = "changes some settings to look like cgy")
    val silentLook by BooleanSetting("Silent Look", false, description = "when activating a look ring only rotate serverside (may lead to desync)")
    val renderStyle by SelectorSetting("ring design", "normal", arrayListOf("normal", "simple", "box"), false, description = "how rings should look")
    val walkFix by SelectorSetting("walk boost", "none", arrayListOf("none", "normal", "big"), false, description = "boost of an edge")
    private val alignedOnly by BooleanSetting("aligned only", false, description = "only lets u use ring that align or while aligned")
    private val blinkShit by DropdownSetting(name = "Blink Settings")
    val speedRings by BooleanSetting(name = "Speed Rings", description = "Toggles the use of tickshift rings").withDependency { blinkShit }
    val blink by DualSetting(name = "actually blink", description = "blink or just movement(yes chloric this was made just for u)", default = false, left = "Movement", right = "Blink").withDependency { blinkShit }
    val mode by DualSetting(name = "movement mode", description = "how movement should look", default = false, left = "Motion", right = "Packet").withDependency { blinkShit }
    val maxBlinks by NumberSetting(name = "max blinks per instance", description = "too much blink on an instance bans apparently", min = 100, max = 300, default = 120).withDependency { blinkShit }
    val resetInterval by NumberSetting(name = "clear intervall", description = "delete packets periodically", min = 1, max = 300, default = 200, unit = "t").withDependency { blinkShit }
    val resetAmount by NumberSetting(name = "clear amount", description = "delete packets periodically", min = 1, max = 400, default = 50).withDependency { blinkShit }
    private val showEnd by BooleanSetting("Render End", default = true, description = "renders waypoint where blink ends").withDependency { blinkShit }
    private val showLine by BooleanSetting("Render Line", default = true, description = "renders line where blink goes").withDependency { blinkShit }
    val moveHud by HudSetting("Move Hud", HudElement(100f, 50f, false, settingName = "Move Hud")).withDependency { blinkShit }
    var customBlinkLengthToggle by BooleanSetting("blink length", default = true, description = "allows for changing the blink length of waypoints").withDependency { blinkShit }
    val customBlinkLength by NumberSetting(name = "length", description = "well how long for the blink to be", min = 1, max = 40, default = 24).withDependency { blinkShit && customBlinkLengthToggle }

    private var rings = mutableMapOf<String, MutableList<Ring>>()
    private var leapedIDs = mutableSetOf<Int>()
    private val deletedRings  = mutableListOf<Ring>()
    private var awaitingLeap = mutableSetOf<Ring>()
    private var awaitingTerm = mutableSetOf<Ring>()
    private var awaitingLeft = mutableSetOf<Ring>()
    private var activatedBlinks = mutableSetOf<BlinkRing>()

    val ringRegistry = AutoP3Utils.discoverRings("noobroutes.features.floor7.autop3.rings")

    private var lastLavaClip = System.currentTimeMillis()
    var isAligned = false

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (!inF7Boss) return
        rings[route]?.forEachIndexed { i, ring ->

            if (alignedOnly && !isAligned && !ring.center && ring !is BlinkRing) return@forEachIndexed

            if (renderIndex) Renderer.drawStringInWorld(i.toString(), ring.coords.add(Vec3(0.0, 0.6, 0.0)), Color.GREEN, depth = depth)

            ring.renderRing()
            if (cgyMode || ring !is BlinkRing) return@forEachIndexed

            val vec3List: List<Vec3> = ring.packets.map { packet -> Vec3(packet.positionX, packet.positionY + 0.01, packet.positionZ) }
            if (showEnd && ring.packets.size > 1) Renderer.drawCylinder(vec3List[vec3List.size-1].add(Vec3(0.0, 0.03, 0.0)),  0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.RED, depth = true)
            if (showLine) RenderUtils.drawGradient3DLine(vec3List, Color.GREEN, Color.RED, 1F, true)
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun tickRing(event: PacketEvent.Send) {
        if (event.packet !is C03PacketPlayer || mc.thePlayer == null) return
        if(!inF7Boss || mc.thePlayer.isSneaking || editMode ) return //|| mc.thePlayer.capabilities.walkSpeed < 0.5

        val bb = mc.thePlayer.entityBoundingBox //cant mc.thePlayer.collided or whatever as i need x AND z collision to align

        val collidesX = mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb.offset(0.001, 0.0, 0.0)).isNotEmpty() ||
                mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb.offset(-0.001, 0.0, 0.0)).isNotEmpty()

        val collidesZ = mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb.offset(0.0, 0.0, 0.001)).isNotEmpty() ||
                mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb.offset(0.0, 0.0, -0.001)).isNotEmpty()

        if (collidesX && collidesZ) { isAligned = true }

        rings[route]?.forEach {ring ->
            val inRing = AutoP3Utils.distanceToRingSq(ring.coords, mc.thePlayer.positionVector) < 0.25 && AutoP3Utils.ringCheckY(ring)

            val isAlignRing = ring.center || ring is BlinkRing

            // as player max speed is 1.4 blocks a tick one check in the middle is enough
            val prevPositionVector = Vec3(mc.thePlayer.prevPosX, mc.thePlayer.prevPosY, mc.thePlayer.prevPosZ)
            val middlePositionVector = mc.thePlayer.positionVector.add(prevPositionVector).multiply(0.5f)
            val passedRing = AutoP3Utils.distanceToRingSq(ring.coords, middlePositionVector) < 0.25 && AutoP3Utils.ringCheckY(ring) //no interpolation on y as i use the fact that it doesnt

            if (inRing || (passedRing && isAlignRing)) {
                if (ring.triggered) return@forEach

                if (passedRing && !inRing) {
                    AutoP3Utils.unPressKeys()
                    PlayerUtils.stopVelocity()
                    event.isCanceled = true
                    mc.thePlayer.setPosition(middlePositionVector.xCoord, mc.thePlayer.posY, middlePositionVector.zCoord)
                    //due to the event/time it doesnt automatically send a c04
                    PacketUtils.sendPacket(C04PacketPlayerPosition(middlePositionVector.xCoord, mc.thePlayer.posY, middlePositionVector.zCoord, mc.thePlayer.onGround))
                }

                if (alignedOnly && !isAligned && !isAlignRing) return@forEach
                ring.triggered = true
                ring.doRingArgs()
                if (ring.left || ring.leap || ring.term) {
                    doAwait(ring)
                    return@forEach
                }
                if (ring is LavaClipRing) {
                    if (System.currentTimeMillis() - lastLavaClip > 1000) {
                        ring.run()
                        lastLavaClip = System.currentTimeMillis()
                    }
                }
                else if (ring !is BlinkRing) ring.run()
                else activatedBlinks.add(ring)
            }
            else {
                ring.triggered = false
                if (ring.leap) awaitingLeap.remove(ring)
                if (ring.term) awaitingTerm.remove(ring)
                if (ring.left) awaitingLeft.remove(ring)
            }
        }
        if (awaitingLeap.isEmpty()) leapedIDs = mutableSetOf<Int>() //this should be done after ring updates
    }

    fun doAwait(ring: Ring) {
        AutoP3Utils.unPressKeys()
        PlayerUtils.stopVelocity()
        if (ring.leap) awaitingLeap.add(ring)
        if (ring.term) awaitingTerm.add(ring)
        if (ring.left) awaitingLeft.add(ring)
    }

    @SubscribeEvent
    fun awaitingOpen(event: TermOpenEvent) {
        awaitingTerm.forEach {
            if (it is BlinkRing)
                activatedBlinks.add(it)
            else it.run()
        }
    }

    @SubscribeEvent
    fun awaitingLeft(event: InputEvent.MouseInputEvent) {
        if (Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()) return

        awaitingLeap.addAll(awaitingTerm) //retard protection (no duplicates)
        awaitingLeap.addAll(awaitingLeft)

        awaitingLeap.forEach {
            if (it is BlinkRing)
                activatedBlinks.add(it)
            else it.run()
        }

        awaitingLeap.clear()
        awaitingTerm.clear()
        awaitingLeft.clear()
    }

    @SubscribeEvent
    fun awaitingLeap(event: PacketEvent.Receive) {
        if (awaitingLeap.isEmpty() || event.packet !is S18PacketEntityTeleport) return
        val entity  = mc.theWorld.getEntityByID(event.packet.entityId)
        if (entity !is EntityPlayer) return

        val x = event.packet.x shr 5 //don't fucking ask why its like this
        val y = event.packet.y shr 5
        val z = event.packet.z shr 5

        if (mc.theWorld.getEntityByID(event.packet.entityId) is EntityPlayer && mc.thePlayer.getDistanceSq(x.toDouble(), y.toDouble(), z.toDouble()) < 5) leapedIDs.add(event.packet.entityId)
        if (leapedIDs.size == leapPlayers()) {
            modMessage("everyone leaped")

            awaitingLeap.forEach {
                if (it is BlinkRing) activatedBlinks.add(it)
                else it.run()
            }

        }
    }

    fun leapPlayers(): Int {
        return when {
            mc.thePlayer.getDistanceSq(2.5, 109.0, 102.5) < 100 -> 3 //ee3
            mc.thePlayer.posY >= 120 -> 1 //core
            else -> 4
        }
    }

    fun handleNoobCommand(args: Array<out String>?) {
        if (route.isEmpty()) return modMessage("Put in a route dumbass")
        when(args?.get(0)) {
            "add","create" -> addNormalRing(args)
            "delete","remove" -> deleteNormalRing(args)
            "blink" -> Blink.blinkCommand(args)
            "edit" -> {
                val ring = if (args.size >= 2) {
                    val selectedIndex = args[1].toIntOrNull() ?: return modMessage("Invalid Index")
                    getRingByIndex(selectedIndex)
                } else getClosestRing(null)
                if (ring == null) return modMessage("No rings found")
                EditUI.openUI(ring)
            }
            "start" -> {
                inF7Boss = true
                BossEventDispatcher.currentBossPhase = Phase.P3
            }
            "rat" -> Utils.rat.forEach{ modMessage(it) }
            "pickup" -> SexAura.pickupLineByName(args)
            "restore" -> restoreRing()
            "test" -> testFunctions(args)
            "load" -> loadRings()
            else -> modMessage("not an option")
        }
    }

    private fun testFunctions(args: Array<out String>) {
        if (args.size < 2) {
            modMessage("Test: rel, relp")
            return
        }
        when(args[1].lowercase()) {
            "relativepos" , "relpos", "rel" -> {
                val blockPos = DungeonUtils.currentRoom?.getRelativeCoords(mc.objectMouseOver.blockPos ?: return devMessage("1")) ?: return devMessage("Not in a room")
                GuiScreen.setClipboardString("BlockPos(${blockPos.x}, ${blockPos.y}, ${blockPos.z})")
                modMessage(blockPos)
            }
            "relativeplayerpos", "relppos", "relplayer", "playerrel", "relp" -> {
                val pos = DungeonUtils.currentRoom?.getRelativeCoords(mc.thePlayer.positionVector) ?: return
                GuiScreen.setClipboardString("Vec3(${pos.xCoord}, ${pos.yCoord}, ${pos.zCoord})")
                modMessage(pos)
            }
            else -> {
                modMessage("All tests passed")
            }
        }
    }

    private fun restoreRing() {
        if (deletedRings.isEmpty())  {
            modMessage("no ring to restore")
            return
        }
        actuallyAddRing(deletedRings.last())
        modMessage("${deletedRings.last().type} added back")
        deletedRings.removeLast()
    }

    private fun addNormalRing(args: Array<out String>?) {
        if (mc.thePlayer == null) return
        if (args == null || args.size < 2) {
            modMessage("Rings: walk, hclip, stop, motion, lava, tnt, jump, speed, clamp, test, insta")
            return
        }
        val coords = mc.thePlayer.positionVector
        val center = args.any {it == "center"}
        val walk = args.any {it == "walk"}
        val term = args.any {it == "term"}
        val leap = args.any { it == "leap" }
        val left = args.any {it == "left"}
        val rotate = args.any {it == "rotate" || it == "look"}
        when (args[1].lowercase()) {
            "walk" -> {
                modMessage("added walk")
                actuallyAddRing(WalkRing(
                    coords,
                    MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
                    term,
                    leap,
                    left,
                    center,
                    rotate
                )
                )
            }

            "hclip" -> {
                modMessage("added hclip")
                actuallyAddRing(HClipRing(
                    coords,
                    MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
                    term,
                    leap,
                    left,
                    center,
                    rotate,
                    walk
                )
                )
            }
            "stop" -> {
                modMessage("added stop")
                actuallyAddRing(StopRing(
                    coords,
                    MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
                    term,
                    leap,
                    left,
                    center,
                    rotate
                )
                )
            }
            "motion" -> {
                if (args.size < 3) {
                    modMessage("need a scale arg (0-1)")
                    return
                }
                val scale = args[2].toFloatOrNull()
                if (scale == null) {
                    modMessage("need a scale arg (0-1)")
                    return
                }
                modMessage("motion added")
                actuallyAddRing(MotionRing(
                    coords,
                    MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
                    term,
                    leap,
                    left,
                    center,
                    rotate,
                    far = walk,
                    scale = scale
                )
                )
            }
            "lava" -> {
                if (args.size < 3) return modMessage("need a length arg")
                val endY = args[2].toDoubleOrNull()?.absoluteValue ?: return modMessage("need a length arg")
                actuallyAddRing(LavaClipRing(
                    coords,
                    MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
                    term,
                    leap,
                    left,
                    center,
                    rotate,
                    endY
                ))

            }
            "tnt", "boom" -> {
                val block = mc.objectMouseOver.blockPos
                if (isAir(block)) {
                    modMessage("must look at a block")
                    return
                }
                modMessage("added boom")
                actuallyAddRing(BoomRing(
                    coords,
                    MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
                    term,
                    leap,
                    left,
                    center,
                    rotate,
                    block
                ))
            }
            "jump" -> {
                modMessage("jump added")
                actuallyAddRing(JumpRing(
                    coords,
                    MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
                    term,
                    leap,
                    left,
                    center,
                    rotate,
                    walk
                ))
            }
            "sped", "speed" -> {
                if (args.size < 3) return modMessage("need a length arg (positive number)")
                val length = args[2].toIntOrNull() ?: return
                if (length < 1) return modMessage("need a number greater than 0")
                modMessage("speed added")
                actuallyAddRing(SpedRing(
                    coords,
                    MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
                    term,
                    leap,
                    left,
                    center,
                    rotate,
                    length
                ))

            }
            "blink" -> {
                if (args.size < 3) return modMessage("need a length arg (positive number)")
                val length = args[2].toIntOrNull() ?: return modMessage("need a length arg (positive number)")
                if (length < 1) return modMessage("need a number greater than 0")
                val waypoint = BlinkWaypoint(
                    coords,
                    MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
                    term,
                    leap,
                    left,
                    center,
                    rotate,
                    length
                )
                waypoint.triggered = true
                blinkStarts.add(waypoint)
            }
            "clamp" -> {
                modMessage("clamp added")
                actuallyAddRing(ClampRing(
                    coords,
                    MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
                    term,
                    leap,
                    left,
                    center,
                    rotate,
                    walk
                ))
            }
            "insta" -> {
                modMessage("insta added")
                actuallyAddRing(InstaRing(
                    coords,
                    MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
                    term,
                    leap,
                    left,
                    center,
                    rotate,
                    walk
                )
                )
            }
            else -> return modMessage("thats not a ring type stoopid")
        }

    }

    fun actuallyAddRing(ring: Ring) {
        ring.triggered = true
        rings[route]?.add(ring) ?: run { rings[route] = mutableListOf(ring) }
        saveRings()
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun doTriggeredBlink(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        activatedBlinks.forEach {
            if (AutoP3Utils.distanceToRingSq(it.coords, mc.thePlayer.positionVector) < 0.25 && AutoP3Utils.ringCheckY(it)) it.run()
            else activatedBlinks.remove(it)
        }
    }

    private fun getClosestRing(distance: Double?): Ring? {
        if (rings[route].isNullOrEmpty()) return null
        val playerEyeVec = mc.thePlayer.positionVector.add(Vec3(0.0, mc.thePlayer.eyeHeight.toDouble(),0.0))
        val ring = rings[route]?.minByOrNull {
            it.coords.squareDistanceTo(playerEyeVec)
        } ?: return null
        return if (distance != null && ring.coords.distanceTo(playerEyeVec) > distance) null else ring
    }

    private fun getRingByIndex(index: Int): Ring? {
        if (rings[route].isNullOrEmpty()) return null
        if (index > rings[route]!!.size - 1) return null
        return rings[route]!![index]
    }


    private fun deleteNormalRing(args: Array<out String>) {
        if (rings[route].isNullOrEmpty()) return

        val ring = if (args.size >= 2) {
            val selectedIndex = args[1].toIntOrNull() ?: return modMessage("Invalid Index")
            getRingByIndex(selectedIndex)
        } else getClosestRing(3.0)

        if (ring == null) return modMessage("No rings found")

        deletedRings.add(ring)
        rings[route]?.remove(ring)
        modMessage("deleted a ${ring.type}")
        saveRings()
    }

    fun saveRings() {
        try {
            val outObj = JsonObject()
            for ((routeName, rings) in rings) {
                val ringArray = JsonArray().apply {
                    for (ring in rings) {
                        add(ring.getAsJsonObject())
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


    fun loadRings() {
        rings.clear()
        try {
            val file = DataManager.loadDataFromFileObject("rings")
            for (route in file) {
                val ringsInJson = mutableListOf<Ring>()
                route.value.forEach {
                    val ring = it.asJsonObject
                    val ringType = ring.get("type")?.asString ?: "Unknown"
                    val ringClass = ringRegistry[ringType]
                    val instance: Ring = ringClass?.java?.getDeclaredConstructor()?.newInstance() ?: return@forEach
                    instance.coords = ring.get("coords").asVec3
                    instance.yaw = MathHelper.wrapAngleTo180_float(ring.get("yaw")?.asFloat ?: 0f)
                    instance.term = ring.get("term")?.asBoolean == true
                    instance.leap = ring.get("leap")?.asBoolean == true
                    instance.center = ring.get("center")?.asBoolean == true
                    instance.rotate = ring.get("rotate")?.asBoolean == true
                    instance.left = ring.get("left")?.asBoolean == true
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
}