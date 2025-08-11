package noobroutes.features.floor7.autop3.rings

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.*
import noobroutes.features.render.FreeCam
import noobroutes.utils.PacketUtils
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage
import kotlin.math.pow


class BlinkRing(
    ringBase: RingBase = RingBase(),
    var packets: List<C04PacketPlayerPosition> = listOf(),
    var endYVelo: Double = 0.0
) : Ring(ringBase, RingType.BLINK) {

    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {
            if (AutoP3.recordingPacketList.isNotEmpty()) {
                modMessage("Why are you calling this while recording a blink")
                return null
            }
            if (args.size < 3) {
                modMessage("need a length arg (positive number)")
                return null
            }

            val length = args[2].toIntOrNull()
            if (length == null) {
                modMessage("need a length arg (positive number)")
                return null
            }

            if (length < 1) {
                modMessage("need a number greater than 0")
                return null
            }

            AutoP3.setActiveBlinkWaypoint(BlinkWaypoint(generateRingBaseFromArgs(args), length).apply { triggered = true })
            return null
        }
    }

    init {
        addDouble("endYVelo", {endYVelo}, {endYVelo = it})
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



    override fun doRing() {
        super.doRing()

        PlayerUtils.stopVelocity()
        mc.thePlayer.isSprinting = false
        if (!FreeCam.enabled) PlayerUtils.unPressKeys()

        if (AutoP3.waitedTicks < AutoP3.blinkCooldown && AutoP3.x_y0uMode) {
            AutoP3.waitedTicks++
            return
        }

        if (!AutoP3.blinkToggle || (AutoP3.blinksThisInstance + packets.size > AutoP3.getMaxBlinks() && AutoP3.isBlinkLimitEnabled) ) {
            doMovement()
            return
        }

        if (AutoP3.cancelled < packets.size) {
            PlayerUtils.stopVelocity()
            return
        }

        devMessage("doing Blink")
        doBlink()
    }

    private fun doMovement() {
        val firstPacket = packets.first()
        val toFar = mc.thePlayer.getDistanceSq(firstPacket.positionX, firstPacket.positionY, firstPacket.positionZ) > ( PlayerUtils.getPlayerWalkSpeed() * 2.806).pow(2)

        if (toFar && AutoP3.cancelled > 0) {
            PacketUtils.sendPacket(C04PacketPlayerPosition(coords.xCoord, mc.thePlayer.posY, coords.zCoord, mc.thePlayer.onGround))
            AutoP3.cancelled--
        }

        AutoP3.setLastMovementedC03(firstPacket)
        AutoP3.movementPackets = packets.toMutableList()
        PlayerUtils.stopVelocity()
        AutoP3.setEndY(endYVelo)
    }

    private fun doBlink() {
        AutoP3.waitedTicks = 0
        val firstPacket = packets.first()
        val toFar = mc.thePlayer.getDistanceSq(firstPacket.positionX, firstPacket.positionY, firstPacket.positionZ) > ( PlayerUtils.getPlayerWalkSpeed() * 2.806).pow(2)

        if (toFar && AutoP3.cancelled > 0) {
            PacketUtils.sendPacket(C04PacketPlayerPosition(coords.xCoord, mc.thePlayer.posY, coords.zCoord, mc.thePlayer.onGround))
            AutoP3.cancelled--
        }

        AutoP3.blinksThisInstance += packets.size
        if (AutoP3.x_y0uMode) AutoP3.cancelled -= packets.size
        else AutoP3.cancelled = 0

        packets.forEach { PacketUtils.sendPacket(it) }

        val lastPacket = packets.last()
        mc.thePlayer.setPosition(lastPacket.positionX, lastPacket.positionY, lastPacket.positionZ)
        mc.thePlayer.setVelocity(0.0, endYVelo, 0.0)

        modMessage("used §c${packets.size}§f packets,  §7(${AutoP3.getMaxBlinks() - AutoP3.blinksThisInstance} left on this instance)")
    }

    fun drawEnd() {
        val lastPacket = packets.last()
        val endCoords = Vec3(lastPacket.positionX, lastPacket.positionY + 0.01, lastPacket.positionZ)
        Renderer.drawCylinder(endCoords, 0.5, 0.5, 0.01, 24, 1, 90, 0, 0, Color.RED, depth = true)
    }

    override fun inRing(pos: Vec3): Boolean {
        return checkInBoundsWithSpecifiedHeight(pos,0f) && mc.thePlayer.onGround
    }
}