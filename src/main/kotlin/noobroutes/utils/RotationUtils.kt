package noobroutes.utils


import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.launch
import net.minecraft.util.BlockPos
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.Core.scope
import noobroutes.utils.skyblock.PlayerUtils.SNEAK_HEIGHT_INVERTED
import kotlin.math.*

object RotationUtils {

    inline val offset get() = ((Scheduler.runTime % 2 * 2 - 1) * 1e-6).toFloat()

    fun yawAndPitchVector(yaw: Float, pitch: Float): Vec3 {
        val f = cos(-yaw * 0.017453292519943295 - PI)
        val f1 = sin(-yaw * 0.017453292519943295 - PI)
        val f2 = -cos(-pitch * 0.017453292519943295)
        val f3 = sin(-pitch * 0.017453292519943295)
        return Vec3(f1*f2, f3, f*f2).bloomNormalize()
    }

    /**
     * Taken from cga
     * @param x X position to aim at.
     * @param y Y position to aim at.
     * @param z Z position to aim at.
     */
    fun getYawAndPitch(x: Double, y: Double, z: Double, sneaking: Boolean = false): Pair<Float, Float> {
        val dx = x - mc.thePlayer.posX
        val dy = y - (mc.thePlayer.posY + mc.thePlayer.eyeHeight - if (sneaking) SNEAK_HEIGHT_INVERTED else 0.0)
        val dz = z - mc.thePlayer.posZ

        val horizontalDistance = sqrt(dx * dx + dz * dz)

        val yaw = Math.toDegrees(atan2(-dx, dz))
        val pitch = -Math.toDegrees(atan2(dy, horizontalDistance))

        val normalizedYaw = if (yaw < -180) yaw + 360 else yaw

        return Pair(normalizedYaw.toFloat(), pitch.toFloat())
    }

    fun getYaw(coords: Vec2i): Float {
        val dx = coords.x - mc.thePlayer.posX
        val dz = coords.z - mc.thePlayer.posZ

        val yaw = Math.toDegrees(atan2(-dx, dz))

        val normalizedYaw = if (yaw < -180) yaw + 360 else yaw

        return normalizedYaw.toFloat()
    }

    fun getYawAndPitchOrigin(originX: Double, originY: Double, originZ: Double, x: Double, y: Double, z: Double, sneaking: Boolean = false): Pair<Float, Float> {
        val dx = x - originX
        val dy = y - (originY + 1.62 - if (sneaking) SNEAK_HEIGHT_INVERTED else 0.0)
        val dz = z - originZ

        val horizontalDistance = sqrt(dx * dx + dz * dz)

        val yaw = Math.toDegrees(atan2(-dx, dz))
        val pitch = -Math.toDegrees(atan2(dy, horizontalDistance))

        val normalizedYaw = if (yaw < -180) yaw + 360 else yaw

        return Pair(normalizedYaw.toFloat(), pitch.toFloat())
    }

    fun getYawAndPitchOrigin(origin: Vec3, target: Vec3, sneaking: Boolean = false): Pair<Float, Float>{
        return getYawAndPitchOrigin(origin.xCoord, origin.yCoord, origin.zCoord, target.xCoord, target.yCoord, target.zCoord, sneaking)
    }

    /**
     * Gets the angle to aim at a Vec3.
     *
     * @param pos Vec3 to aim at.
     */
    fun getYawAndPitch(pos: Vec3, sneaking: Boolean = false): Pair<Float, Float> {
        return getYawAndPitch(pos.xCoord, pos.yCoord, pos.zCoord, sneaking)
    }


    fun setAngles(yaw: Float?, pitch: Float?) {
        yaw?.let { mc.thePlayer.rotationYaw = yaw }
        pitch?.let { mc.thePlayer.rotationPitch = pitch.coerceIn(-90f, 90f) }
    }

    fun setAngleToVec3(vec3: Vec3, sneaking: Boolean = false) {
        val angles = getYawAndPitch(vec3.xCoord, vec3.yCoord, vec3.zCoord, sneaking)
        setAngles(angles.first, angles.second)
    }
}