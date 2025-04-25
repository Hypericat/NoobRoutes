package com.github.wadey3636.noobroutes.utils


import com.github.wadey3636.noobroutes.features.Blink
import me.noobmodcore.Core.mc
import me.noobmodcore.utils.skyblock.PlayerUtils
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
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

    /**
     * Queues a rotation with specified yaw, pitch, and clicks.
     *
     * @param yaw The horizontal rotation angle.
     * @param pitch The vertical rotation angle.
     * @param silent Indicates whether the rotation should be silent. Defaults to false.
     */
    fun clickAt(yaw: Float, pitch: Float, silent: Boolean = false) {
        queuedRots.add(Rotation(yaw, pitch, true, silent))
    }

    private val offset get() = ((Scheduler.runTime % 2 * 2 - 1) * 1e-6).toFloat()


    private var packetSent = false


    @SubscribeEvent
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        if (event.phase != TickEvent.Phase.END || event.player != mc.thePlayer || mc.thePlayer == null || event.isCanceled || queuedRots.isEmpty()) return
        val rot = queuedRots.removeFirst()
        Blink.rotSkip = true
        if (rot.silent) {
            SilentRotator.doSilentRotation()
        }
        setAngles(rot.yaw + offset, rot.pitch)
        if (rot.click) {
            Scheduler.schedulePostTickTask {
                PlayerUtils.airClick()
            }
        }

    }





}