package noobroutes.features.floor7

import noobroutes.Core.logger
import noobroutes.Core.mc
import noobroutes.config.DataManager
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.TermOpenEvent
import noobroutes.features.Blink
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.misc.SexAura
import noobroutes.features.move.LavaClip
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.*
import noobroutes.ui.hud.HudElement
import noobroutes.utils.*
import noobroutes.utils.adapters.RingsMapTypeAdapter
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.modMessage
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S18PacketEntityTeleport
import net.minecraft.util.Timer
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.Core
import noobroutes.Core.mc
import noobroutes.features.Blink.lastBlink
import noobroutes.features.Blink.lastBlinkRing
import noobroutes.mixin.accessors.TimerFieldAccessor
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.render.RenderUtils
import noobroutes.utils.skyblock.devMessage
import org.lwjgl.input.Keyboard
import kotlin.concurrent.timer

enum class RingTypes {
    WALK,
    YEET,
    HCLIP,
    STOP,
    BLINK,
    TERM,
    LEAP,
    MOTION,
    LAVA,
    TNT,
    JUMP,
    SPED
}



data class Ring (
    val type: RingTypes,
    val coords: Vec3 = Vec3(mc.thePlayer?.posX ?: 0.0, mc.thePlayer?.posY ?: 0.0, mc.thePlayer?.posZ ?: 0.0),
    val direction: LookVec = LookVec(mc.thePlayer?.rotationYaw ?: 0f, mc.thePlayer?.rotationPitch ?: 0f),
    val walk: Boolean = false,
    val look: Boolean = false,
    val center: Boolean = false,
    var should: Boolean = false,
    val blinkPackets: List<C04PacketPlayerPosition> = emptyList(),
    val endY: Double = 0.0
)

object AutoP3: Module (
    name = "AutoP3",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "AutoP3"
) {
    val gson = GsonBuilder().registerTypeAdapter(
        object : TypeToken<MutableMap<String, MutableList<Ring>>>() {}.type,
        RingsMapTypeAdapter()
    ).setPrettyPrinting().create()

    private val route by StringSetting("Route", "", description = "Route to use")
    val editMode by BooleanSetting("Edit Mode", false, description = "Disables ring actions")
    val depth by BooleanSetting("Depth Check", true, description = "Makes rings render through walls")
    private val renderIndex by BooleanSetting("Render Index", false, description = "Renders the index of the ring. Useful for creating routes")
    val frame by BooleanSetting("Check per Frame", false, description = "check each frame if the player is in a ring. Routes are easier to setup with per frame but possibly less consistent on low fps. Per tick is harder to setup but 100% consistent. Everything done on frame can also be done on tick")
    val motionValue by NumberSetting(name = "motion value", description = "how much yeet to put into the motion", min = 0f, max = 1000f, default = 509f)
    private val silentLook by BooleanSetting("Silent Look", false, description = "when activating a look ring only rotate serverside (may lead to desync)")
    val simpleRings by BooleanSetting("Simple Rings", false, description = "switches complicated rings with simple circles")
    val onlyCenter by BooleanSetting("Only Starts", false, description = "only renders rings with the center property(should be only start rings) and blinks")
    private val blinkShit by DropdownSetting(name = "Blink Settings")
    val blink by DualSetting(name = "actually blink", description = "blink or just movement(yes chloric this was made just for u)", default = false, left = "Movement", right = "Blink").withDependency { blinkShit }
    val mode by DualSetting(name = "movement mode", description = "how movement should look", default = false, left = "Motion", right = "Packet").withDependency { blinkShit }
    val maxBlinks by NumberSetting(name = "max blinks per instance", description = "too much blink on an instance bans apparently", min = 100, max = 300, default = 120).withDependency { blinkShit }
    val showEnd by BooleanSetting("Render End", default = true, description = "renders waypoint where blink ends").withDependency { blinkShit }
    val showLine by BooleanSetting("Render Line", default = true, description = "renders line where blink goes").withDependency { blinkShit }
    val moveHud by HudSetting("Move Hud", HudElement(100f, 50f, false, settingName = "Move Hud")).withDependency { blinkShit }
    var customBlinkLengthToggle by BooleanSetting("blink length", default = true, description = "allows for changing the blink length of waypoints").withDependency { blinkShit }
    val customBlinkLength by NumberSetting(name = "length", description = "well how long for the blink to be", min = 1, max = 40, default = 24).withDependency { blinkShit && customBlinkLengthToggle }
    val timerSpeed by NumberSetting(name = "sped ring speed", description = "how much faster it goes (100 means 100x speed) also need tick check for high speeds", min = 2f, max = 100f, default = 10f).withDependency { blinkShit }
    val toggleSG by BooleanSetting("SG toggle", default = false, description = "Disable Secret guide in boss")

    private var rings = mutableMapOf<String, MutableList<Ring>>()
    var waitingTerm = false
    var waitingLeap = false
    private var leapedIDs = mutableSetOf<Int>()
    var inBoss = false
    private val deletedRings  = mutableListOf<Ring>()
    var spedFor = 0

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if(!inBoss) return
        rings[route]?.forEachIndexed { i, ring ->
            if (renderIndex) Renderer.drawStringInWorld(i.toString(), ring.coords.add(Vec3(0.0, 0.6, 0.0)), Color.GREEN, depth = depth)
            AutoP3Utils.renderRing(ring)
            if (ring.type == RingTypes.BLINK) {
                val vec3List: List<Vec3> = ring.blinkPackets.map { packet -> Vec3(packet.positionX, packet.positionY + 0.01, packet.positionZ) }
                if (showEnd && ring.blinkPackets.size > 1) Renderer.drawCylinder(vec3List[vec3List.size-1].add(Vec3(0.0, 0.03, 0.0)),  0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.RED, depth = true)
                if (showLine) RenderUtils.drawGradient3DLine(vec3List, Color.GREEN, Color.RED, 1F, true)
            }
            if (editMode || !frame) return@forEachIndexed
            if (AutoP3Utils.distanceToRingSq(ring.coords) < 0.25 && AutoP3Utils.ringCheckY(ring) && ring.should) {
                executeRing(ring)
                if (ring.type != RingTypes.BLINK) ring.should = false
            }
            else if(AutoP3Utils.distanceToRingSq(ring.coords) > 0.25 || !AutoP3Utils.ringCheckY(ring)) ring.should = true
        }
        waitingTerm = rings[route]?.any { it.type == RingTypes.TERM && !it.should } == true
        waitingLeap = rings[route]?.any { it.type == RingTypes.LEAP && !it.should } == true
        if (!waitingLeap) leapedIDs = mutableSetOf<Int>()
    }

    @SubscribeEvent
    fun tickRing(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END || frame) return
        if(!inBoss) return
        rings[route]?.forEachIndexed { i, ring ->
            if (AutoP3Utils.distanceToRingSq(ring.coords) < 0.25 && AutoP3Utils.ringCheckY(ring) && ring.should) {
                executeRing(ring)
                if (ring.type != RingTypes.BLINK) ring.should = false
            }
            else if(AutoP3Utils.distanceToRingSq(ring.coords) > 0.25 || !AutoP3Utils.ringCheckY(ring)) ring.should = true
        }
        waitingTerm = rings[route]?.any { it.type == RingTypes.TERM && !it.should } == true
        waitingLeap = rings[route]?.any { it.type == RingTypes.LEAP && !it.should } == true
        if (!waitingLeap) leapedIDs = mutableSetOf<Int>()
    }

    @SubscribeEvent
    fun onStart(event: PacketEvent.Receive) {
        if (event.packet !is S08PacketPlayerPosLook) return
        if (event.packet.x == 73.5 && event.packet.y == 221.5 && event.packet.z == 14.5) {
            if (toggleSG && !inBoss) SecretGuideIntegration.setSecretGuideAura(false)
            inBoss = true
        }
    }

    private fun executeRing(ring: Ring) {
        if (ring.look) {
            if (!silentLook) mc.thePlayer.rotationYaw = ring.direction.yaw
            Blink.rotate = ring.direction.yaw
        }
        if (ring.center && mc.thePlayer.onGround) {
            mc.thePlayer.setPosition(ring.coords.xCoord, mc.thePlayer.posY, ring.coords.zCoord)
            Blink.rotSkip = true
        }
        stopOrNot(ring)
        AutoP3Utils.walkAfter = ring.walk && ring.type != RingTypes.JUMP
        if (ring.walk && ring.type == RingTypes.JUMP) AutoP3Utils.startWalk(ring.direction.yaw)
        when(ring.type) {
            RingTypes.WALK -> {
                AutoP3Utils.startWalk(ring.direction.yaw)
                modMessage("walking")
            }
            RingTypes.STOP -> {
                modMessage("stopping")
                mc.thePlayer.motionX = 0.0
                mc.thePlayer.motionZ = 0.0
            }
            RingTypes.HCLIP -> {
                modMessage("hclipping")
                mc.thePlayer.motionX = 0.0
                mc.thePlayer.motionZ = 0.0
                if(mc.thePlayer.onGround) mc.thePlayer.jump()
                AutoP3Utils.awaitingTick = true
                AutoP3Utils.direction = ring.direction.yaw
            }
            RingTypes.BLINK -> {
                if (lastBlinkRing == ring && System.currentTimeMillis() - lastBlink < 5000) return
                Blink.doBlink(ring)
            }
            RingTypes.TERM -> {
                modMessage("waiting")
                mc.thePlayer.motionX = 0.0
                mc.thePlayer.motionZ = 0.0
                AutoP3Utils.direction = ring.direction.yaw
            }
            RingTypes.LEAP -> {
                modMessage("waiting")
                mc.thePlayer.motionX = 0.0
                mc.thePlayer.motionZ = 0.0
                AutoP3Utils.direction = ring.direction.yaw
            }
            RingTypes.YEET -> {
                modMessage("yeeting")
                AutoP3Utils.direction = ring.direction.yaw
                AutoP3Utils.yeetTicks = 0
                AutoP3Utils.yeeting = true
            }
            RingTypes.MOTION -> {
                modMessage("motioning")
                mc.thePlayer.motionX = 0.0
                mc.thePlayer.motionZ = 0.0
                if(mc.thePlayer.onGround) mc.thePlayer.jump()
                AutoP3Utils.awaitingTick = true
                AutoP3Utils.direction = ring.direction.yaw
            }
            RingTypes.LAVA -> {
                modMessage("activating lava clip")
                mc.thePlayer.motionX = 0.0
                mc.thePlayer.motionZ = 0.0
                LavaClip.ringClip = ring.endY
                LavaClip.toggle()
            }
            RingTypes.TNT -> {
                modMessage("tnting")
                SwapManager.swapFromName("TNT")
                if (ring.blinkPackets.isEmpty()) {
                    modMessage("how tf is this empty, send this in noobroutes dc")
                    return
                }
                Scheduler.schedulePreTickTask(1) { AuraManager.auraBlock(ring.blinkPackets[0].positionX.toInt(), ring.blinkPackets[0].positionY.toInt(), ring.blinkPackets[0].positionZ.toInt(), force = true) }
            }
            RingTypes.JUMP -> {
                modMessage("jumping")
                if (mc.thePlayer.onGround) mc.thePlayer.jump()
                if (ring.walk) AutoP3Utils.startWalk(ring.direction.yaw)
            }
            RingTypes.SPED -> {
                if (ring.endY > Blink.cancelled || spedFor > 0) return
                if (ring.endY < 1.0) {
                    modMessage("Broken Speed Ring, cancelling execution")
                    return

                }

                modMessage("speeding (solid trip)")
                AutoP3Utils.setGameSpeed(timerSpeed)
                spedFor = ring.endY.toInt()
                modMessage(spedFor)
            }
            else -> modMessage("how tf did u manage to get a ring like this")
        }
    }


    @SubscribeEvent
    fun spedTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END || spedFor == 0) return
        spedFor--
        if (spedFor == 0) AutoP3Utils.setGameSpeed(1f)
    }

    fun stopOrNot(ring: Ring) {
        if (ring.type == RingTypes.TNT ||
            (ring.type == RingTypes.BLINK && lastBlinkRing == ring && System.currentTimeMillis() - lastBlink < 5000) ||
            ring.type == RingTypes.SPED
        ) return
        else AutoP3Utils.unPressKeys()
    }

    @SubscribeEvent
    fun awaitingOpen(event: TermOpenEvent) {
        if (!waitingTerm) return
        AutoP3Utils.walking = true
    }

    @SubscribeEvent
    fun awaitingLeap(event: PacketEvent.Receive) {
        if (!waitingLeap || event.packet !is S18PacketEntityTeleport) return
        val entity  = mc.theWorld.getEntityByID(event.packet.entityId)
        if (entity !is EntityPlayer) return
        val x = event.packet.x/32.0 //don't fucking ask why its like this
        val y = event.packet.y/32.0
        val z = event.packet.z/32.0
        if (mc.theWorld.getEntityByID(event.packet.entityId) is EntityPlayer && mc.thePlayer.getDistanceSq(x,y,z) < 1.25) leapedIDs.add(event.packet.entityId)
        if (leapedIDs.size == leapPlayers()) {
            modMessage("everyone leaped")
            AutoP3Utils.walking = true
        }
    }

    fun leapPlayers(): Int {
        return if (mc.thePlayer.getDistanceSq(2.5, 109.0, 102.5) < 2) 3 //ee3 spot (core)
        else if (mc.thePlayer.posY >= 120) 1 //premine leap
        else 4
    }

    fun handleNoobCommand(args: Array<out String>?) {
        if (route.isEmpty()) return modMessage("Put in a route dumbass")
        when(args?.get(0)) {
            "add","create" -> addNormalRing(args)
            "delete","remove" -> deleteNormalRing(args)
            "blink" -> Blink.blinkCommand(args)
            "start" -> inBoss = true
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
            "relativepos" -> {
                modMessage(DungeonUtils.currentRoom?.getRelativeCoords(mc.objectMouseOver.blockPos))
            }
            "speed" -> {
                if (args.size < 3) return
                val speed = args[2].toFloatOrNull() ?: return
                AutoP3Utils.setGameSpeed(speed)
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
        if (args == null || args.size < 2) {
            modMessage("Rings: walk, hclip, stop, term, leap, yeet, motion")
            return
        }
        val ringType: RingTypes
        var endPos = 0.0
        var packets = mutableListOf<C04PacketPlayerPosition>()
        when(args[1].lowercase()) {
            "walk" -> {
                modMessage("added walk")
                ringType = RingTypes.WALK
            }
            "hclip" -> {
                modMessage("added hclip")
                ringType = RingTypes.HCLIP
            }
            "stop" -> {
                modMessage("added stop")
                ringType = RingTypes.STOP
            }
            "term" -> {
                modMessage("added await term")
                ringType = RingTypes.TERM
                AutoP3Utils.direction = mc.thePlayer.rotationYaw
            }
            "leap" -> {
                modMessage("added await leap")
                ringType = RingTypes.LEAP
                AutoP3Utils.direction = mc.thePlayer.rotationYaw
            }
            "yeet" -> {
                modMessage("yeet added")
                ringType = RingTypes.YEET
            }
            "motion" -> {
                modMessage("motion added")
                ringType = RingTypes.MOTION
            }
            "lava" -> {
                if (args.size < 3) {
                    modMessage("need a length arg (positive number)")
                    return
                }
                ringType = RingTypes.LAVA
                val endY = args[2].toDoubleOrNull()
                if (endY == null) {
                    modMessage("need a length arg (positive number)")
                    return
                }
                endPos = endY
            }
            "tnt", "boom" -> {
                ringType = RingTypes.TNT
                val block = mc.objectMouseOver.blockPos
                if (isAir(block)) {
                    modMessage("must look at a block")
                    return
                }
                modMessage("added boom")
                packets.add(C04PacketPlayerPosition(block.x.toDouble(), block.y.toDouble(), block.z.toDouble(), false))
            }
            "jump" -> {
                modMessage("jump added")
                ringType = RingTypes.JUMP
            }
            "sped", "speed" -> {
                if (args.size < 3) {
                    modMessage("need a length arg (positive number)")
                    return
                }
                val length = args[2].toDoubleOrNull() ?: return
                if (length <= 0) {
                    modMessage("positive number pls")
                    return
                }
                modMessage("im sped")
                ringType = RingTypes.SPED
                endPos = length
            }
            else -> return modMessage("thats not a ring type stoopid")
        }
        val look = args.any { it == "look" }
        val center = args.any {it == "center"}
        val walk = args.any {it == "walk"} && ringType != RingTypes.WALK
        actuallyAddRing(Ring(ringType, look = look, center = center, walk = walk, endY = endPos, blinkPackets = packets))
    }

    fun actuallyAddRing(ring: Ring) {
        rings[route]?.add(ring) ?: run { rings[route] = mutableListOf(ring) }
        saveRings()
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
            val jsonArray = JsonArray().apply {
                rings.forEach { (routeName, ringList) ->
                    add(JsonObject().apply {
                        addProperty("route", routeName)
                        add("rings", gson.toJsonTree(ringList))
                    })
                }
            }
            DataManager.saveDataToFile("rings", jsonArray)
        } catch (e: Exception) {
            modMessage("error saving")
            logger.error("error saving rings", e)
        }
    }

    fun loadRings(){
        rings.clear()
        val jsonObjects = DataManager.loadDataFromFile("rings")
        jsonObjects.forEach { jsonObject ->
            val routeName = jsonObject.get("route").asString
            val ringList = gson.fromJson<MutableList<Ring>>(jsonObject.get("rings"), object : TypeToken<MutableList<Ring>>() {}.type)
            rings[routeName] = ringList
        }
    }
}