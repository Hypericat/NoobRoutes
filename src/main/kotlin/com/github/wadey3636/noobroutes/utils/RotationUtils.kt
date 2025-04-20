package com.github.wadey3636.noobroutes.utils


import me.modcore.Core.mc
import net.minecraft.util.Vec3
import kotlin.math.atan2
import kotlin.math.sqrt

object RotationUtils {
    private const val SNEAKHEIGHT = 0.0800000429153443;

    /**
     * Taken from cga
     * @param x X position to aim at.
     * @param y Y position to aim at.
     * @param z Z position to aim at.
     * @param sneaking determines whether the function accounts for sneak height.
     */
    fun getYawAndPitch(x: Double, y:Double, z:Double, sneaking: Boolean = false): Pair<Float, Float> {
        val dx = x - mc.thePlayer.posX
        val dy = y - (mc.thePlayer.posY + mc.thePlayer.eyeHeight - if (sneaking) SNEAKHEIGHT else 0.0)
        val dz = z - mc.thePlayer.posZ

        val horizontalDistance = sqrt(dx * dx + dz * dz )

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
     * Taken from cga
     * @param x X position to aim at.
     * @param y Y position to aim at.
     * @param z Z position to aim at.
     * @
     */
    fun getYawAndPitchOrigin(origin: Vec3, pos: Vec3): Pair<Float, Float> {
        val dx = pos.xCoord - origin.xCoord
        val dy = pos.yCoord - origin.yCoord
        val dz = pos.zCoord - origin.zCoord

        val horizontalDistance = sqrt(dx * dx + dz * dz )

        val yaw = Math.toDegrees(atan2(-dx, dz))
        val pitch = -Math.toDegrees(atan2(dy, horizontalDistance))

        val normalizedYaw = if (yaw < -180) yaw + 360 else yaw

        return Pair(normalizedYaw.toFloat(), pitch.toFloat())
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
    fun setAngleToVec3(vec3: Vec3, sneaking: Boolean = false){
        val angles = getYawAndPitch(vec3.xCoord, vec3.yCoord, vec3.zCoord, sneaking)
        setAngles(angles.first, angles.second)

    }





}