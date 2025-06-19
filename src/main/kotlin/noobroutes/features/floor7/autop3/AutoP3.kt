package noobroutes.features.floor7.autop3

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.server.S18PacketEntityTeleport
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.Core.logger
import noobroutes.config.DataManager
import noobroutes.events.BossEventDispatcher.inF7Boss
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.TermOpenEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.floor7.autop3.Blink.blinkStarts
import noobroutes.features.floor7.autop3.rings.*
import noobroutes.features.misc.SexAura
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.*
import noobroutes.ui.hud.HudElement
import noobroutes.utils.*
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.render.Color
import noobroutes.utils.render.RenderUtils
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse


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
    val renderIndex by BooleanSetting("Render Index", false, description = "Renders the index of the ring. Useful for creating routes")
    val fasterMotion by BooleanSetting("faster motion", false, description = "doesnt stop before the jump for motion")
    var noRotate by BooleanSetting("no rotate", false, description = "forces the player to be unable to change where they look in boss. Pretty much only way to gurantee working motion rings")
    private val noRotateKey by KeybindSetting("toggle no rotate", Keyboard.KEY_NONE, "toggles no rotate setting").onPress {
        noRotate = !noRotate
        modMessage("can rotate: " + !noRotate)
    }
    val cgyMode by BooleanSetting("cgy Mode", false, description = "changes some settings to look like cgy")
    val silentLook by BooleanSetting("Silent Look", false, description = "when activating a look ring only rotate serverside (may lead to desync)")
    val fuckingLook by BooleanSetting("Loud Look", false, description = "always look for if u want to make ur autop3 seem mroe legit or smth")
    val renderStyle by SelectorSetting("ring design", "normal", arrayListOf("normal", "simple", "box"), false, description = "how rings should look")
    val onlyCenter by BooleanSetting("Only Starts", false, description = "walking of edges doesnt give u a boost forward")
    val walkFix by BooleanSetting("walk fix", false, description = "always look for if u want to make ur autop3 seem mroe legit or smth")
    private val blinkShit by DropdownSetting(name = "Blink Settings")
    val speedRings by BooleanSetting(name = "Speed Rings", description = "Toggles the use of tickshift rings").withDependency { blinkShit }
    val blink by DualSetting(name = "actually blink", description = "blink or just movement(yes chloric this was made just for u)", default = false, left = "Movement", right = "Blink").withDependency { blinkShit }
    val mode by DualSetting(name = "movement mode", description = "how movement should look", default = false, left = "Motion", right = "Packet").withDependency { blinkShit }
    val maxBlinks by NumberSetting(name = "max blinks per instance", description = "too much blink on an instance bans apparently", min = 100, max = 300, default = 120).withDependency { blinkShit }
    val showEnd by BooleanSetting("Render End", default = true, description = "renders waypoint where blink ends").withDependency { blinkShit }
    val showLine by BooleanSetting("Render Line", default = true, description = "renders line where blink goes").withDependency { blinkShit }
    val moveHud by HudSetting("Move Hud", HudElement(100f, 50f, false, settingName = "Move Hud")).withDependency { blinkShit }
    var customBlinkLengthToggle by BooleanSetting("blink length", default = true, description = "allows for changing the blink length of waypoints").withDependency { blinkShit }
    val customBlinkLength by NumberSetting(name = "length", description = "well how long for the blink to be", min = 1, max = 40, default = 24).withDependency { blinkShit && customBlinkLengthToggle }
    val timerSpeed by NumberSetting(name = "sped ring speed", description = "how much faster it goes (100 means 100x speed) also need tick check for high speeds", min = 2f, max = 100f, default = 10f).withDependency { blinkShit }
    private val testShit by DropdownSetting(name = "test ring")
    val tick0 by NumberSetting(name = "0", description = "tick 0 speed", min = 1.4, max = 1.5, default = 1.0, increment = 0.01).withDependency { testShit }
    val tick1 by NumberSetting(name = "1", description = "tick 1 speed", min = 3.0, max = 3.1, default = 1.0, increment = 0.01).withDependency { testShit }
    val tick2 by NumberSetting(name = "2", description = "tick 2 speed", min = 1.95, max = 2.1, default = 1.0, increment = 0.01).withDependency { testShit }
    val tick3 by NumberSetting(name = "3", description = "tick 3 speed", min = 1.8, max = 1.9, default = 1.0, increment = 0.01).withDependency { testShit }
    val tick4 by NumberSetting(name = "4", description = "tick 4 speed", min = 1.65, max = 1.75, default = 1.0, increment = 0.01).withDependency { testShit }
    val tick5 by NumberSetting(name = "5", description = "tick 5 speed", min = 1.55, max = 1.65, default = 1.0, increment = 0.01).withDependency { testShit }
    val tick6 by NumberSetting(name = "6", description = "tick 6 speed", min = 1.4, max = 1.55, default = 1.0, increment = 0.01).withDependency { testShit }
    val tick7 by NumberSetting(name = "7", description = "tick 7 speed", min = 1.3, max = 1.45, default = 1.0, increment = 0.01).withDependency { testShit }
    val tick8 by NumberSetting(name = "8", description = "tick 8 speed", min = 1.2, max = 1.35, default = 1.0, increment = 0.01).withDependency { testShit }
    val tick9 by NumberSetting(name = "9", description = "tick 9 speed", min = 1.15, max = 1.3, default = 1.0, increment = 0.01).withDependency { testShit }
    val tick10 by NumberSetting(name = "10", description = "tick 10 speed", min = 1.05, max = 1.15, default = 1.0, increment = 0.01).withDependency { testShit }
    val tick11 by NumberSetting(name = "11", description = "tick 11 speed", min = 0.0, max = 1.1, default = 1.0, increment = 0.01).withDependency { testShit }
    val tick12 by NumberSetting(name = "12", description = "tick 12 speed", min = 0.0, max = 1.1, default = 1.0, increment = 0.01).withDependency { testShit }
    val tick13 by NumberSetting(name = "13", description = "tick 13 speed", min = 0.0, max = 1.1, default = 1.0, increment = 0.01).withDependency { testShit }
    val tick14 by NumberSetting(name = "14", description = "tick 14 speed", min = 0.0, max = 1.1, default = 1.0, increment = 0.01).withDependency { testShit }
    val tick15 by NumberSetting(name = "15", description = "tick 15 speed", min = 0.0, max = 1.1, default = 1.0, increment = 0.01).withDependency { testShit }


    private var rings = mutableMapOf<String, MutableList<Ring>>()
    private var leapedIDs = mutableSetOf<Int>()
    private val deletedRings  = mutableListOf<Ring>()
    var spedFor = 0
    private var awaitingLeap = mutableSetOf<Ring>()
    private var awaitingTerm = mutableSetOf<Ring>()
    private var awaitingLeft = mutableSetOf<Ring>()
    private var activatedBlinks = mutableSetOf<BlinkRing>()

    val ringRegistry = AutoP3Utils.discoverRings("noobroutes.features.floor7.autop3.rings")


    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (!inF7Boss) return
        rings[route]?.forEachIndexed { i, ring ->
            if (renderIndex) Renderer.drawStringInWorld(i.toString(), ring.coords.add(Vec3(0.0, 0.6, 0.0)), Color.GREEN, depth = depth)
            AutoP3Utils.renderRing(ring)
            if (ring !is BlinkRing || cgyMode) return@forEachIndexed
            val vec3List: List<Vec3> = ring.packets.map { packet -> Vec3(packet.positionX, packet.positionY + 0.01, packet.positionZ) }
            if (showEnd && ring.packets.size > 1) Renderer.drawCylinder(vec3List[vec3List.size-1].add(Vec3(0.0, 0.03, 0.0)),  0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.RED, depth = true)
            if (showLine) RenderUtils.drawGradient3DLine(vec3List, Color.GREEN, Color.RED, 1F, true)
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun tickRing(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        if(!inF7Boss || mc.thePlayer.isSneaking) return
        rings[route]?.forEach {ring ->
            if (editMode) return@forEach
            val inRing = AutoP3Utils.distanceToRingSq(ring.coords) < 0.25 && AutoP3Utils.ringCheckY(ring)
            if (inRing && !ring.triggered) {
                ring.triggered = ring !is BlinkRing || ring.left || ring.leap || ring.term
                ring.doRingArgs()
                if (ring.leap || ring.term || ring.left) {
                    AutoP3Utils.unPressKeys()
                    mc.thePlayer.motionX = 0.0
                    mc.thePlayer.motionZ = 0.0
                }
                if (ring.leap) {
                    awaitingLeap.add(ring)
                    return@forEach
                }
                if (ring.term) {
                    awaitingTerm.add(ring)
                    return@forEach
                }
                if (ring.left) {
                    awaitingLeft.add(ring)
                    return@forEach
                }
                ring.run()
            }
            else if (!inRing) {
                ring.triggered = false
                if (ring.leap) awaitingLeap.remove(ring)
                if (ring.term) awaitingTerm.remove(ring)
                if (ring.left) awaitingLeft.remove(ring)
            }
        }
        if (awaitingLeap.isEmpty()) leapedIDs = mutableSetOf<Int>()
    }


    @SubscribeEvent
    fun spedTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END || spedFor == 0) return
        spedFor--
        if (spedFor == 0) AutoP3Utils.setGameSpeed(1f)
    }


    @SubscribeEvent
    fun awaitingOpen(event: TermOpenEvent) {
        if (awaitingTerm.isEmpty()) return
        awaitingTerm.forEach {
            if (it is BlinkRing) activatedBlinks.add(it)
            else it.run()
        }
    }

    @SubscribeEvent
    fun onLeftMouse(event: InputEvent.MouseInputEvent) {
        if (awaitingLeap.isEmpty() && awaitingTerm.isEmpty() && awaitingLeft.isEmpty()) return
        val isLeft = Mouse.getEventButton() == 0
        if (!isLeft || !Mouse.getEventButtonState()) return
        awaitingLeap.addAll(awaitingTerm) //retard protection (no duplicates)
        awaitingLeap.addAll(awaitingLeft)
        awaitingLeap.forEach {
            if (it is BlinkRing) activatedBlinks.add(it)
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
        val x = event.packet.x/32.0 //don't fucking ask why its like this
        val y = event.packet.y/32.0
        val z = event.packet.z/32.0
        if (mc.theWorld.getEntityByID(event.packet.entityId) is EntityPlayer && mc.thePlayer.getDistanceSq(x,y,z) < 5) leapedIDs.add(event.packet.entityId)
        if (leapedIDs.size == leapPlayers()) {
            modMessage("everyone leaped")
            awaitingLeap.forEach {
                if (it is BlinkRing) activatedBlinks.add(it)
                else it.run()
            }
        }
    }

    fun leapPlayers(): Int {
        return if (mc.thePlayer.getDistanceSq(2.5, 109.0, 102.5) < 10) 3 //ee3 spot (core)
        else if (mc.thePlayer.posY >= 120) 1 //premine leap
        else 4
    }

    fun handleNoobCommand(args: Array<out String>?) {
        if (route.isEmpty()) return modMessage("Put in a route dumbass")
        when(args?.get(0)) {
            "add","create" -> addNormalRing(args)
            "delete","remove" -> deleteNormalRing(args)
            "blink" -> Blink.blinkCommand(args)
            "start" -> inF7Boss = true
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
            modMessage("Test: sgToggle, roomName, relativePos")
            return
        }
        when(args[1].lowercase()) {
            "sgtoggle" -> {
                SecretGuideIntegration.setSecretGuideAura(false)
            }
            "roomname" -> {
                modMessage(DungeonUtils.currentRoomName)
            }
            "relativepos" , "relpos", "rel" -> {
                val blockPos = DungeonUtils.currentRoom?.getRelativeCoords(mc.objectMouseOver.blockPos ?: return devMessage("1")) ?: return devMessage("2")
                GuiScreen.setClipboardString("BlockPos(${blockPos.x}, ${blockPos.y}, ${blockPos.z})")
                modMessage(blockPos)
            }
            "relativeplayerpos", "relppos", "relplayer", "playerrel" -> {
                val pos = DungeonUtils.currentRoom?.getRelativeCoords(mc.thePlayer.positionVector) ?: return
                GuiScreen.setClipboardString("Vec3(${pos.xCoord}, ${pos.yCoord}, ${pos.zCoord})")
                modMessage(pos)
            }
            /*"speed" -> {
                if (args.size < 3) return
                val speed = args[2].toFloatOrNull() ?: return
                AutoP3Utils.setGameSpeed(speed)
            }*/
            "motion" -> {
                if (args.size < 4) return
                val add = args[2].toIntOrNull() ?: return
                val mult = args[3].toDoubleOrNull() ?: return
                AutoP3Utils.jump1 = add
                AutoP3Utils.jump2 = mult
                modMessage("1 $add 2 $mult")
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
            modMessage("Rings: walk, hclip, stop, term, leap, yeet, motion")
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
                    mc.thePlayer.rotationYaw,
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
                    mc.thePlayer.rotationYaw,
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
                    mc.thePlayer.rotationYaw,
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
                    mc.thePlayer.rotationYaw,
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
                if (args.size < 3) {
                    modMessage("need a length arg (positive number)")
                    return
                }
                val endY = args[2].toDoubleOrNull()
                if (endY == null) {
                    modMessage("need a length arg (positive number)")
                    return
                }
                actuallyAddRing(LavaClipRing(
                    coords,
                    mc.thePlayer.rotationYaw,
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
                    mc.thePlayer.rotationYaw,
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
                    mc.thePlayer.rotationYaw,
                    term,
                    leap,
                    left,
                    center,
                    rotate,
                    walk
                ))
            }
            "sped", "speed" -> {
                if (args.size < 3) {
                    modMessage("need a length arg (positive number)")
                    return
                }
                val length = args[2].toIntOrNull() ?: return
                if (length < 1) {
                    modMessage("need a number greater than 0")
                    return
                }
                modMessage("im sped")
                actuallyAddRing(SpedRing(
                    coords,
                    mc.thePlayer.rotationYaw,
                    term,
                    leap,
                    left,
                    center,
                    rotate,
                    length
                ))

            }
            "blink" -> {
                if (args.size < 3) {
                    modMessage("need a length arg (positive number)")
                    return
                }
                val length = args[2].toIntOrNull()
                if (length == null){
                    modMessage("need a length arg (positive number)")
                    return
                }
                if (length < 1) {
                    modMessage("need a number greater than 0")
                    return
                }
                val waypoint = BlinkWaypoint(
                    coords,
                    mc.thePlayer.rotationYaw,
                    term,
                    leap,
                    left,
                    center,
                    rotate,
                    length,
                    walk
                )
                waypoint.triggered = true
                blinkStarts.add(waypoint)
            }
            "clamp" -> {
                modMessage("clamp added")
                actuallyAddRing(ClampRing(
                    coords,
                    mc.thePlayer.rotationYaw,
                    term,
                    leap,
                    left,
                    center,
                    rotate,
                    walk
                ))
            }
            "test" -> {
                modMessage("test added")
                actuallyAddRing(TestRing(
                    coords,
                    mc.thePlayer.rotationYaw,
                    term,
                    leap,
                    left,
                    center,
                    rotate
                )
                )
            }
            "insta" -> {
                modMessage("added insta")
                actuallyAddRing(InstaRing(
                    coords,
                    mc.thePlayer.rotationYaw,
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
        if (event.phase != TickEvent.Phase.START || activatedBlinks.isEmpty()) return
        for (ring in activatedBlinks) {
            if (AutoP3Utils.distanceToRingSq(ring.coords) < 0.25 && AutoP3Utils.ringCheckY(ring)) {
                ring.run()
            }
            else {
                activatedBlinks.remove(ring)
            }
        }
    }

    private fun deleteNormalRing(args: Array<out String>) {
        if (rings[route].isNullOrEmpty()) return
        if (args.size >= 2) {
            try {
                if ((args[1].toInt()) <= (rings[route]?.size?.minus(1) ?: return modMessage("Error Deleting Ring"))) {
                    deletedRings.add(rings[route]?.get(args[1].toInt())!!)
                    rings[route]?.removeAt(args[1].toInt())
                    modMessage("Removed Ring ${args[1].toInt()}")
                    saveRings()
                    return
                } else {
                    modMessage("Invalid Index")
                    return
                }
            } catch (e: NumberFormatException) {
                modMessage("Invalid Index")
                return
            }
        }

        val playerEyeVec = mc.thePlayer.positionVector.add(Vec3(0.0, mc.thePlayer.eyeHeight.toDouble(),0.0))
        val deleteList = rings[route]?.sortedBy{it.coords.squareDistanceTo(playerEyeVec)}
        if (deleteList?.get(0)?.coords?.squareDistanceTo(playerEyeVec)!! > 9) return
        deletedRings.add(deleteList[0])
        rings[route]?.remove(deleteList[0])
        modMessage("deleted a ring")
        saveRings()
    }

    fun saveRings(){
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
            val fileType = DataManager.fileType("rings") ?: return
            if (fileType == "JsonPrimitive" || fileType == "JsonNull") return
            if (fileType == "JsonArray") {
                val file = DataManager.loadDataFromFileArray("rings")
                file.forEach { route ->
                    val name = route.get("route").asString
                    val ringsList = mutableListOf<Ring>()
                    route.get("rings").asJsonArray.forEach {
                        val obj = it.asJsonObject
                        val type = obj.get("type").asString
                        val positionType = obj.get("coords")

                        val coords = when {
                            positionType.isJsonObject -> {
                                val positionObj = positionType.asJsonObject
                                Vec3(
                                    positionObj.get("field_72450_a")?.asDouble ?: obj.get("x")?.asDouble ?: 0.0,
                                    positionObj.get("field_72448_b")?.asDouble ?: obj.get("y")?.asDouble ?: 0.0,
                                    positionObj.get("field_72449_c")?.asDouble ?: obj.get("z")?.asDouble ?: 0.0
                                )
                            }

                            positionType.isJsonArray -> {
                                val arr = positionType.asJsonArray
                                if (arr.size() > 0) {
                                    Vec3(
                                        arr[0]?.asDouble ?: 0.0,
                                        arr[1]?.asDouble ?: 0.0,
                                        arr[2]?.asDouble ?: 0.0
                                    )
                                } else {
                                    Vec3(obj.get("x")?.asDouble ?: 0.0, obj.get("y")?.asDouble ?: 0.0, obj.get("z")?.asDouble ?: 0.0)
                                }

                            }

                            else -> {
                                Vec3(0.0, 0.0, 0.0)
                            }
                        }


                        val dirElem = obj.get("direction") ?: obj.get("directions")
                        val direction: LookVec = if (dirElem != null) when {
                            dirElem.isJsonObject -> {
                                val o = dirElem.asJsonObject
                                val yaw = o.get("yaw")?.asFloat ?: o.get("field_yaw")?.asFloat ?: 0f
                                val pitch = o.get("pitch")?.asFloat ?: o.get("field_pitch")?.asFloat ?: 0f
                                LookVec(yaw, pitch)
                            }
                            dirElem.isJsonArray -> {
                                LookVec(obj.get("yaw").asFloat, obj.get("pitch").asFloat)
                            }
                            else -> {
                                LookVec(0f, 0f)
                            }
                        } else {
                            LookVec(0f, 0f)
                        }

                        val walk = obj.get("walk")?.asBoolean ?: false
                        val look = obj.get("look")?.asBoolean ?: false
                        val center = obj.get("center")?.asBoolean ?: false
                        val misc = obj.get("misc")?.asDouble ?: obj.get("endY")?.asDouble ?: 0.0
                        val blinks = mutableListOf<C04PacketPlayerPosition>()
                        if (obj.has("blinkPackets") || obj.has("blink_packets")) {
                            val arr =
                                obj.getAsJsonArray(if (obj.has("blinkPackets")) "blinkPackets" else "blink_packets")
                            arr.forEach { el ->
                                val p = el.asJsonObject
                                val x = p.get("x")?.asDouble ?: p.get("field_149479_a")?.asDouble ?: 0.0
                                val y = p.get("y")?.asDouble ?: p.get("field_149477_b")?.asDouble ?: 0.0
                                val z = p.get("z")?.asDouble ?: p.get("field_149478_c")?.asDouble ?: 0.0
                                val g = p.get("onGround")?.asBoolean ?: p.get("field_149474_g")?.asBoolean ?: false
                                blinks.add(C04PacketPlayerPosition(x, y, z, g))
                            }
                        }
                        when (type) {
                            "YEET" -> {
                                ringsList.add(MotionRing(coords, direction.yaw,
                                    term = false,
                                    leap = false,
                                    left = false,
                                    center = center,
                                    rotate = look
                                ))
                            }

                            "WALK" -> {
                                ringsList.add(WalkRing(coords, direction.yaw,
                                    term = false,
                                    leap = false,
                                    left = false,
                                    center = center,
                                    rotate = look
                                ))
                            }

                            "STOP" -> {
                                ringsList.add(StopRing(coords, direction.yaw,
                                    term = false,
                                    leap = false,
                                    left = false,
                                    center = center,
                                    rotate = look
                                ))
                            }

                            "HCLIP" -> {
                                ringsList.add(HClipRing(coords, direction.yaw,
                                    term = false,
                                    leap = false,
                                    left = false,
                                    center = center,
                                    rotate = look,
                                    walk = walk
                                ))
                            }

                            "BLINK" -> {
                                ringsList.add(
                                    BlinkRing(
                                        coords,
                                        direction.yaw,
                                        false,
                                        false,
                                        left = false,
                                        center = center,
                                        rotate = look,
                                        packets = blinks,
                                        endYVelo = misc
                                    )
                                )
                            }

                            "LAVA" -> {
                                ringsList.add(
                                    LavaClipRing(
                                        coords,
                                        direction.yaw,
                                        false,
                                        leap = false,
                                        left = false,
                                        center = center,
                                        rotate = look,
                                        length = misc
                                    )
                                )
                            }

                            "TERM" -> {
                                ringsList.add(WalkRing(coords, direction.yaw,
                                    term = true,
                                    leap = false,
                                    left = false,
                                    center = center,
                                    rotate = look
                                ))
                            }

                            "LEAP" -> {
                                ringsList.add(WalkRing(coords, direction.yaw,
                                    term = false,
                                    leap = true,
                                    left = false,
                                    center = center,
                                    rotate = look
                                ))
                            }

                            "TNT" -> {
                                ringsList.add(
                                    BoomRing(
                                        coords,
                                        direction.yaw,
                                        term = false,
                                        leap = false,
                                        left = false,
                                        center = center,
                                        rotate = look,
                                        block = BlockPos(blinks[0].positionX, blinks[0].positionY, blinks[0].positionZ)
                                    )
                                )
                            }
                            "JUMP" -> {
                                ringsList.add(JumpRing(coords, direction.yaw,
                                    term = false,
                                    leap = false,
                                    left = false,
                                    center = center,
                                    rotate = look,
                                    walk = walk
                                ))
                            }
                            "SPED" -> {
                                ringsList.add(SpedRing(coords, direction.yaw,
                                    term = false,
                                    leap = false,
                                    left = false,
                                    center = center,
                                    rotate = look,
                                    length = misc.toInt()
                                ))
                            }
                        }
                    }
                    rings[name] = ringsList
                }
                saveRings()
            } else {
                val file = DataManager.loadDataFromFileObject("rings")
                for (route in file) {
                    val ringsInJson = mutableListOf<Ring>()
                    route.value.forEach {
                        val ring = it.asJsonObject
                        val ringType = ring.get("type")?.asString ?: "Unknown"
                        val ringClass = ringRegistry[ringType]
                        val instance: Ring = ringClass?.java?.getDeclaredConstructor()?.newInstance() ?: return@forEach
                        instance.coords = ring.get("coords").asVec3
                        instance.yaw = ring.get("yaw")?.asFloat ?: 0f
                        instance.term = ring.get("term")?.asBoolean ?: false
                        instance.leap = ring.get("leap")?.asBoolean ?: false
                        instance.center = ring.get("center")?.asBoolean ?: false
                        instance.rotate = ring.get("rotate")?.asBoolean ?: false
                        instance.left = ring.get("left")?.asBoolean ?: false
                        instance.loadRingData(ring)
                        ringsInJson.add(instance)
                    }
                    rings[route.key] = ringsInJson
                }
            }
        } catch (e: Exception) {
            modMessage("Error Loading Rings, Please Send Log to Wadey")
            logger.info(e)
        }
    }
}