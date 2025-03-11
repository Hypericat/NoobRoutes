package com.github.wadey3636.noobroutes.features

import com.github.wadey3636.noobroutes.utils.AutoP3Utils
import com.github.wadey3636.noobroutes.utils.Utils
import com.github.wadey3636.noobroutes.utils.adapters.RingsMapTypeAdapter
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import me.defnotstolen.Core.logger
import me.defnotstolen.Core.mc
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.utils.skyblock.modMessage
import net.minecraft.util.Vec3
import org.lwjgl.input.Keyboard
import me.defnotstolen.config.DataManager
import me.defnotstolen.events.impl.PacketEvent
import me.defnotstolen.features.settings.impl.BooleanSetting
import me.defnotstolen.features.settings.impl.NumberSetting
import me.defnotstolen.features.settings.impl.StringSetting
import me.defnotstolen.utils.LookVec
import me.defnotstolen.utils.render.Color
import me.defnotstolen.utils.render.Renderer
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.server.S18PacketEntityTeleport
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

enum class RingTypes {
    WALK,
    YEET,
    HCLIP,
    STOP,
    BLINK,
    TERM,
    LEAP
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
    val motionValue by NumberSetting(name = "motion value", description = "how much yeet to put into the motion", min = 0f, max = 1000f, default = 100f)
    private var rings = mutableMapOf<String, MutableList<Ring>>()
    private var waitingTerm = false
    private var waitingLeap = false
    private var leaped = 0

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        rings[route]?.forEachIndexed { i, ring ->
            if (renderIndex) Renderer.drawStringInWorld(i.toString(), ring.coords.add(Vec3(0.0, 0.6, 0.0)), Color.GREEN, depth = depth)
            AutoP3Utils.renderRing(ring)
            if (ring.type == RingTypes.BLINK) {
                val vec3List: List<Vec3> = ring.blinkPackets.map { packet -> Vec3(packet.positionX, packet.positionY, packet.positionZ) }
                if (Blink.showEnd && ring.blinkPackets.size > 1) Renderer.drawCylinder(vec3List[vec3List.size-1].add(Vec3(0.0, 0.03, 0.0)),  0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.RED, depth = true)
                if (Blink.showLine) Renderer.draw3DLine(vec3List, Color.GREEN, lineWidth = 1F, depth = true)
            }
            if (editMode) return@forEachIndexed
            if (AutoP3Utils.distanceToRing(ring.coords) < 0.5 && AutoP3Utils.ringCheckY(ring) && ring.should) {
                executeRing(ring)
                if (ring.type != RingTypes.BLINK) ring.should = false
            }
            else if(AutoP3Utils.distanceToRing(ring.coords) > 0.5 || !AutoP3Utils.ringCheckY(ring)) ring.should = true
        }
        waitingTerm = rings[route]?.any { it.type == RingTypes.TERM && !it.should } == true
        waitingLeap = rings[route]?.any { it.type == RingTypes.LEAP && !it.should } == true
        if (!waitingLeap) leaped = 0
    }

    private fun executeRing(ring: Ring) {
        if (ring.look) {
            mc.thePlayer.rotationYaw = ring.direction.yaw
            Blink.rotate = ring.direction.yaw
        }
        if (ring.center && mc.thePlayer.onGround) mc.thePlayer.setPosition(ring.coords.xCoord, mc.thePlayer.posY, ring.coords.zCoord)
        if (ring.walk) AutoP3Utils.walkAfter = true
        AutoP3Utils.unPressKeys()
        when(ring.type) {
            RingTypes.WALK -> {
                AutoP3Utils.startWalk(ring.direction.yaw)
                modMessage("started Walking")
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
                Blink.doBlink(ring)
            }
            RingTypes.TERM -> {
                mc.thePlayer.motionX = 0.0
                mc.thePlayer.motionZ = 0.0
                AutoP3Utils.direction = ring.direction.yaw
            }
            RingTypes.LEAP -> {
                mc.thePlayer.motionX = 0.0
                mc.thePlayer.motionZ = 0.0
                AutoP3Utils.direction = ring.direction.yaw
            }
            RingTypes.YEET -> {
                val speed = mc.thePlayer.capabilities.walkSpeed * 0.1
                mc.thePlayer.motionX = speed * -Utils.xPart(ring.direction.yaw)
                mc.thePlayer.motionZ = speed * -Utils.zPart(ring.direction.yaw)
                AutoP3Utils.direction = ring.direction.yaw
                AutoP3Utils.yeetTicks = 1
                AutoP3Utils.yeeting = true
            }
            else -> modMessage("how tf did u manage to get a ring like this")
        }
    }

    @SubscribeEvent
    fun awaitingOpen(event: PacketEvent.Receive) {
        if (!waitingTerm || event.packet !is S2DPacketOpenWindow) return
        AutoP3Utils.walking = true
    }

    @SubscribeEvent
    fun awaitingLeap(event: PacketEvent.Receive) {
        if (!waitingLeap || event.packet !is S18PacketEntityTeleport) return
        val x = event.packet.x.toDouble()
        val y = event.packet.y.toDouble()
        val z = event.packet.z.toDouble()
        if (mc.theWorld.getEntityByID(event.packet.entityId) is EntityPlayer && mc.thePlayer.getDistance(x,y,z) < 2) leaped++
        if (leaped == 4) {
            modMessage("everyone leaped")
            AutoP3Utils.walking = true
        }
    }

    fun addRing(args: Array<out String>?) {
        if (route.isEmpty()) return modMessage("Put in a route dumbass")
        when(args?.get(0)) {
            "add" -> addNormalRing(args)
            "delete" -> deleteNormalRing(args)
            "remove" -> deleteNormalRing(args)
            "blink" -> Blink.blinkCommand(args)
            else -> modMessage("not an option")
        }
    }

    private fun addNormalRing(args: Array<out String>?) {
        val ringType: RingTypes
        when(args?.get(1)?.lowercase()) { //dear kotlin, if u check the line above u see that i am checking wether args[1] is null. Pls stop complaining about args[1] possibly being null
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
            }
            "leap" -> {
                modMessage("added await leap")
                ringType = RingTypes.LEAP
            }
            "yeet" -> {
                modMessage("yeet added")
                ringType = RingTypes.YEET
            }
            else -> return modMessage("thats not a ring type stoopid")
        }
        val look = args.any { it == "look" }
        val center = args.any {it == "center"}
        val walk = args.any {it == "walk"} && ringType != RingTypes.WALK
        actuallyAddRing(Ring(ringType, look = look, center = center, walk = walk))
        saveRings()
    }

    fun actuallyAddRing(ring: Ring) {
        rings[route]?.add(ring) ?: run { rings[route] = mutableListOf(ring) }
    }

    private fun deleteNormalRing(args: Array<out String>) {
        if (rings[route].isNullOrEmpty()) return
        if (args.size >= 2) {
            try {
                if ((args[1].toInt()) <= (rings[route]?.size?.minus(1) ?: return modMessage("Error Deleting Ring"))) {
                    rings[route]?.removeAt(args[1].toInt())
                    modMessage("Removed Ring ${args[1].toInt()}")
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
        val deleteList = rings[route]?.sortedBy{it.coords.distanceTo(playerEyeVec)}
        if (deleteList?.get(0)?.coords?.distanceTo(playerEyeVec)!! > 3) return
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