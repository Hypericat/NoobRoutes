package noobroutes.features.floor7.autop3.rings

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.AutoP3
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

@RingType("Blink")
class BlinkRing(
    coords: Vec3 = Vec3(0.0, 0.0, 0.0),
    yaw: Float = 0f,
    term: Boolean = false,
    leap: Boolean = false,
    left: Boolean = false,
    center: Boolean = false,
    rotate: Boolean = false,
    var packets: List<C04PacketPlayerPosition> = listOf(),
    var endYVelo: Double = 0.0
) : Ring(coords, yaw, term, leap, left, center, rotate) {


    init {
        addDouble("endYVelo", {endYVelo}, {endYVelo = it})
    }

    override fun loadRingData(obj: JsonObject) {
        val packetsLoaded = mutableListOf<C04PacketPlayerPosition>()
        obj.get("packets").asJsonArray.forEach {
            val packet = it.asJsonObject
            packetsLoaded.add(C04PacketPlayerPosition(packet.get("x").asDouble, packet.get("y").asDouble, packet.get("z").asDouble, packet.get("isOnGround").asBoolean))
        }
        packets = packetsLoaded
    }


    override fun addRingData(obj: JsonObject) {
        obj.apply {
            add("packets", JsonArray().apply {
                packets.forEach {
                    add(JsonObject().apply {
                        addProperty("x", it.positionX)
                        addProperty("y", it.positionY)
                        addProperty("z", it.positionZ)
                        addProperty("isOnGround", it.isOnGround)
                    })
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
        
        if (!canBlinkNow || notEnoughPackets) {
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
            return
        }
        
        if (exceedsBlinkLimit || blinkDisabled) {
            modMessage("movementing")
            movementPackets = packets.toMutableList()
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionY = 0.0
            endY = endYVelo
            lastBlink = System.currentTimeMillis()
            lastBlinkRing = null
            return
        }
        
        blinksInstance += packets.size
        lastBlink = System.currentTimeMillis()
        lastBlinkRing = this
        packets.forEach { PacketUtils.sendPacket(it) }
        val lastPacket = packets.last()
        mc.thePlayer.setPosition(lastPacket.positionX, lastPacket.positionY, lastPacket.positionZ)
        mc.thePlayer.setVelocity(0.0, endYVelo, 0.0)
        modMessage("§c§l$cancelled§r§f c04s available, used §c${packets.size}§f,  §7(${AutoP3.maxBlinks - blinksInstance} left on this instance)")
    }
}