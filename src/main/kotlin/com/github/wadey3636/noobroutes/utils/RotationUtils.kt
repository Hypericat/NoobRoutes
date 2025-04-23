package com.github.wadey3636.noobroutes.utils


import com.github.wadey3636.noobroutes.features.Blink
import me.noobmodcore.Core.mc
import me.noobmodcore.events.impl.PacketEvent
import me.noobmodcore.utils.skyblock.PlayerUtils
import me.noobmodcore.utils.skyblock.devMessage
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.*
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.atan2
import kotlin.math.sqrt

object RotationUtils {
    private const val SNEAKHEIGHT = 0.0800000429153443

    class Rotation(val yaw: Float, val pitch: Float, val click: Boolean = false, val silent: Boolean = false)

    private var queuedRots = mutableListOf<Rotation>()

    /**
     * Taken from cga
     * @param x X position to aim at.
     * @param y Y position to aim at.
     * @param z Z position to aim at.
     * @param sneaking determines whether the function accounts for sneak height.
     */
    fun getYawAndPitch(x: Double, y: Double, z: Double, sneaking: Boolean = false): Pair<Float, Float> {
        val dx = x - mc.thePlayer.posX
        val dy = y - (mc.thePlayer.posY + mc.thePlayer.eyeHeight - if (sneaking) SNEAKHEIGHT else 0.0)
        val dz = z - mc.thePlayer.posZ

        val horizontalDistance = sqrt(dx * dx + dz * dz)

        val yaw = Math.toDegrees(atan2(-dx, dz))
        val pitch = -Math.toDegrees(atan2(dy, horizontalDistance))

        val normalizedYaw = if (yaw < -180) yaw + 360 else yaw

        return Pair(normalizedYaw.toFloat(), pitch.toFloat())
    }

    /**
     * Gets the angle to aim at a Vec3.
     *
     * @param pos Vec3 to aim at.
     * @param sneaking determines whether the function accounts for sneak height.
     */
    fun getYawAndPitch(pos: Vec3, sneaking: Boolean = false): Pair<Float, Float> {
        return getYawAndPitch(pos.xCoord, pos.yCoord, pos.zCoord, sneaking)
    }


    /**
     * sets angle from relative yaw/pitch
     * @param {number} yaw delta yaw
     * @param {number} pitch delta pitch
     */
    fun setAngles(yaw: Float, pitch: Float) {
        mc.thePlayer.rotationYaw = yaw
        mc.thePlayer.rotationPitch = pitch.coerceIn(-90f, 90f)
    }

    /**
     * sets angle to a vec3
     */
    fun setAngleToVec3(vec3: Vec3, sneaking: Boolean = false) {
        val angles = getYawAndPitch(vec3.xCoord, vec3.yCoord, vec3.zCoord, sneaking)
        setAngles(angles.first, angles.second)

    }

    private var lastSentRot: Pair<Float, Float> = Pair(0f, 0f)

    fun rotateTo(yaw: Float, pitch: Float, silent: Boolean = false) {
        queuedRots.add(Rotation(yaw, pitch, false, silent))
    }

    fun clickAt(yaw: Float, pitch: Float, silent: Boolean = false) {
        queuedRots.add(Rotation(yaw, pitch, true, silent))
    }


    private var packetSent = false


    @SubscribeEvent
    fun onPacketSent(event: PacketEvent.Send) {
        if (event.packet !is C03PacketPlayer) return
        if (packetSent) {
            packetSent = false
            return
        }
        if (queuedRots.isEmpty()) {
            return
        }
        val rot = queuedRots.removeFirst()
        if (rot.yaw == lastSentRot.first && rot.pitch == lastSentRot.second) {
            if (rot.click) PlayerUtils.airClick()
            return
        }
        event.isCanceled = true
        packetSent = true
        Blink.rotSkip = true
        val packet = event.packet
        if (packet is C04PacketPlayerPosition || packet is C06PacketPlayerPosLook) {
            PacketUtils.sendPacket(
                C06PacketPlayerPosLook(
                    packet.positionX,
                    packet.positionY,
                    packet.positionZ,
                    rot.yaw,
                    rot.pitch,
                    packet.isOnGround
                )
            )
            devMessage("Sent C06PacketPlayerPosLook, ${rot.yaw}, ${rot.pitch}, ${packet.isOnGround}")
            if (!rot.silent) {
               ClientUtils.clientScheduleTask { setAngles(rot.yaw, rot.pitch) }
            }
            if (rot.click) PlayerUtils.airClick()
        } else {
            PacketUtils.sendPacket(
                C05PacketPlayerLook(
                    rot.yaw,
                    rot.pitch,
                    packet.isOnGround
                )
            )
            devMessage("Sent C06PacketPlayerPosLook, ${rot.yaw}, ${rot.pitch}, ${packet.isOnGround}")
            if (!rot.silent) {
                ClientUtils.clientScheduleTask { setAngles(rot.yaw, rot.pitch) }
            }
            if (rot.click) PlayerUtils.airClick()
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = false)
    fun onLastPacketSent(event: PacketEvent.Send){
        if (event.packet is C05PacketPlayerLook || event.packet is C06PacketPlayerPosLook) {
            lastSentRot = Pair(event.packet.yaw, event.packet.pitch)
        }
    }

}