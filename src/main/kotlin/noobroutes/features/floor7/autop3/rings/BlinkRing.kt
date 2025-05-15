package noobroutes.features.floor7.autop3.rings

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
import noobroutes.utils.AutoP3Utils
import noobroutes.utils.PacketUtils
import noobroutes.utils.skyblock.modMessage

class BlinkRing(
    coords: Vec3,
    yaw: Float = 0f,
    term: Boolean = false,
    leap: Boolean = false,
    left: Boolean = false,
    center: Boolean = false,
    rotate: Boolean = false,
    val packets: List<C04PacketPlayerPosition>,
    val endYVelo: Double
) : Ring("Blink", coords, yaw, term, leap, left, center, rotate) {


    override fun addRingData(obj: JsonObject) {
        obj.apply {
            add("packets", JsonObject().apply {
                packets.forEach {
                    addProperty("x", it.positionX)
                    addProperty("y", it.positionY)
                    addProperty("z", it.positionZ)
                    addProperty("isOnGround", it.isOnGround)
                }
            })
            addProperty("endYVelo", endYVelo)
        }
    }

    override fun doRing() {
        if (movementPackets.isNotEmpty()) return
        val hasEnoughPackets = cancelled >= packets.size
        val hasEnoughLeftOnInstance = blinksInstance + packets.size > AutoP3.maxBlinks
        val notOnCd = System.currentTimeMillis() - lastBlink >= 500

        AutoP3Utils.unPressKeys()

        if ((!hasEnoughPackets || !notOnCd) && !(!hasEnoughLeftOnInstance || !AutoP3.blink)) {
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
            return
        }

        if (!hasEnoughLeftOnInstance || !AutoP3.blink) {
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
        //lastBlinkRing = ring
        packets.forEach { PacketUtils.sendPacket(it) }
        val lastPacket = packets[packets.size - 1]
        mc.thePlayer.setPosition(lastPacket.positionX, lastPacket.positionY, lastPacket.positionZ)
        mc.thePlayer.setVelocity(0.0, endYVelo, 0.0)
        modMessage("§c§l$cancelled§r§f c04s available, used §c${packets.size}§f,  §7(${AutoP3.maxBlinks - blinksInstance} left on this instance)")
    }
}