package noobroutes.features.floor7.autop3.rings

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.Blink
import noobroutes.features.floor7.autop3.Blink.blinksInstance
import noobroutes.features.floor7.autop3.Blink.cancelled
import noobroutes.features.floor7.autop3.Blink.endY
import noobroutes.features.floor7.autop3.Blink.lastBlink
import noobroutes.features.floor7.autop3.Blink.lastBlinkRing
import noobroutes.features.floor7.autop3.Blink.movementPackets
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.AutoP3Utils
import noobroutes.utils.PacketUtils
import noobroutes.utils.skyblock.modMessage
import kotlin.math.pow

@RingType("Blink")
class BlinkRing(
    coords: Vec3 = Vec3(0.0, 0.0, 0.0),
    yaw: Float = 0f,
    term: Boolean = false,
    leap: Boolean = false,
    left: Boolean = false,
    center: Boolean = false,
    rotate: Boolean = false,
    diameter: Float = 1f,
    height: Float = 1f,
    var packets: List<C04PacketPlayerPosition> = listOf(),
    var endYVelo: Double = 0.0
) : Ring(coords, yaw, term, leap, left, center, rotate, diameter, height) {


    init {
        addDouble("endYVelo", {endYVelo}, {endYVelo = it})
    }

    override fun loadRingData(obj: JsonObject) {
        super.loadRingData(obj)
        val packetsLoaded = mutableListOf<C04PacketPlayerPosition>()
        obj.get("packets").asJsonArray.forEach {
            if (it.isJsonPrimitive) {
                val packetInfoArray = it.asString.split(", ")
                packetsLoaded.add(C04PacketPlayerPosition(packetInfoArray[0].toDouble(), packetInfoArray[1].toDouble(), packetInfoArray[2].toDouble(), packetInfoArray[3].toBoolean()))
            } else {
                val packet = it.asJsonObject
                packetsLoaded.add(C04PacketPlayerPosition(packet.get("x").asDouble, packet.get("y").asDouble, packet.get("z").asDouble, packet.get("isOnGround").asBoolean))
            }
        }
        packets = packetsLoaded
    }

    override fun ringCheckY(): Boolean {
        return coords.yCoord == mc.thePlayer.posY && mc.thePlayer.onGround
    }


    override fun addRingData(obj: JsonObject) {
        obj.apply {
            add("packets", JsonArray().apply {
                packets.forEach { packet ->
                    add(JsonPrimitive("${packet.positionX}, ${packet.positionY}, ${packet.positionZ}, ${packet.isOnGround}"))
                }
            })
        }
    }

    override fun doRing() {
        if (movementPackets.isNotEmpty()) return

        AutoP3Utils.unPressKeys()

        val canBlinkNow = System.currentTimeMillis() - lastBlink >= 500
        val notEnoughPackets = cancelled < packets.size
        val exceedsBlinkLimit = blinksInstance + packets.size > AutoP3.maxBlinks
        val blinkDisabled = !AutoP3.blink

        val firstPacket = packets.first()
        val toFar = mc.thePlayer.getDistanceSq(firstPacket.positionX, firstPacket.positionY, firstPacket.positionZ) > (mc.thePlayer.capabilities.walkSpeed * 2.806).pow(2)
        
        if (!canBlinkNow || (notEnoughPackets && !exceedsBlinkLimit)) {
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
            return
        }

        if (toFar && cancelled > 0) {
            PacketUtils.sendPacket(C04PacketPlayerPosition(coords.xCoord, mc.thePlayer.posY, coords.zCoord, mc.thePlayer.onGround))
            cancelled--
        }
        
        if (exceedsBlinkLimit || blinkDisabled) {
            if (AutoP3.renderStyle == 3) modMessage("Moving", "§0[§6Yharim§0]§7 ")
            movementPackets = packets.toMutableList()
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
            endY = endYVelo
            lastBlink = System.currentTimeMillis()
            lastBlinkRing = null
            return
        }
        
        blinksInstance += packets.size
        Blink.cancelled -= packets.size
        lastBlink = System.currentTimeMillis()
        lastBlinkRing = this
        packets.forEach { PacketUtils.sendPacket(it) }
        val lastPacket = packets.last()
        mc.thePlayer.setPosition(lastPacket.positionX, lastPacket.positionY, lastPacket.positionZ)
        AutoP3.isAligned = true
        mc.thePlayer.setVelocity(0.0, endYVelo, 0.0)
        if (AutoP3.renderStyle == 3) modMessage("Blinking", "§0[§6Yharim§0]§7 ")
        else modMessage("§c§l$cancelled§r§f c04s available, used §c${packets.size}§f,  §7(${AutoP3.maxBlinks - blinksInstance} left on this instance)")
    }
}