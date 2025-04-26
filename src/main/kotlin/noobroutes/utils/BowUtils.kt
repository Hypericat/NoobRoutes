package noobroutes.utils

import noobroutes.Core.mc
import noobroutes.features.render.ClickGUIModule.devMode
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.projectile.EntityArrow
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object BowUtils {

    private const val RANGE = 40

    private val linesList = mutableListOf<ArrayList<Vec3>>()
    /**
     * Adapted from odin.
     *
     *
     * Calculates and identifies entities hit in the path of a projectile based on the given origin,
     * yaw, and pitch. Determines the trajectory starting from the specified origin point with
     * an angle defined by yaw and pitch, and simulates the motion of the projectile to detect collisions.
     *
     * @param origin The starting position of the projectile, represented as a vector.
     * @param yaw The horizontal angle (in degrees) defining the direction of the projectile.
     * @param pitch The vertical angle (in degrees) defining the trajectory elevation of the projectile.
     * @return A list of entities hit by the simulated projectile trajectory, or null if no entities are hit.
     */
    fun findHitEntity(origin: Vec3, yaw: Double, pitch:Double): List<Entity>? {
        val charge = 2f

        val yawRadians = Math.toRadians(yaw)
        val pitchRadians = Math.toRadians(pitch)

        val posX = origin.xCoord - cos(Math.toRadians(yaw)) * 0.16
        val posY = origin.yCoord
        val posZ = origin.zCoord - sin(Math.toRadians(pitch)) * 0.16

        var motionX = -sin(yawRadians) * cos(pitchRadians)
        var motionY = -sin(pitchRadians)
        var motionZ = cos(yawRadians) * cos(pitchRadians)

        val lengthOffset = sqrt(motionX * motionX + motionY * motionY + motionZ * motionZ)
        motionX = (motionX / lengthOffset) * charge * 1.5
        motionY = (motionY / lengthOffset) * charge * 1.5
        motionZ = (motionZ / lengthOffset) * charge * 1.5

        return calculateBowTrajectory(Vec3(motionX, motionY, motionZ), Vec3(posX, posY, posZ))
    }

    /**
     * Adapted from odin
     */
    private fun calculateBowTrajectory(mV: Vec3, pV: Vec3): List<Entity>? {
        var motionVec = mV
        var posVec = pV
        val lines = arrayListOf<Vec3>()
        repeat(RANGE + 1) {
            lines.add(posVec)
            val aabb = AxisAlignedBB(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
                .offset(posVec.xCoord, posVec.yCoord, posVec.zCoord)
                .addCoord(motionVec.xCoord, motionVec.yCoord, motionVec.zCoord)
                .expand(0.01, 0.01, 0.01)
            val entityHit = mc.theWorld?.getEntitiesWithinAABBExcludingEntity(mc.thePlayer, aabb)
                ?.filter { it !is EntityArrow && it !is EntityArmorStand }.orEmpty()
            if (entityHit.isNotEmpty()) {
                if (!linesList.any { it.containsAll(lines) && it.size == lines.size }) linesList.add(lines)
                return entityHit
            } else {
                mc.theWorld?.rayTraceBlocks(posVec, motionVec.add(posVec), true, false, false)?.let {
                    if (!linesList.any { it.containsAll(lines) && it.size == lines.size }) linesList.add(lines)
                    return null
                }
            }
            posVec = posVec.add(motionVec)
            motionVec = Vec3(motionVec.xCoord * 0.99, motionVec.yCoord * 0.99 - 0.05, motionVec.zCoord * 0.99)
        }
        if (!linesList.any { it.containsAll(lines) && it.size == lines.size }) linesList.add(lines)
        return null
    }

    @SubscribeEvent
    fun debug(event: RenderWorldLastEvent){
        if (!devMode) return
        linesList.toList().forEach {
            Renderer.draw3DLine(it, Color.GREEN)
        }
    }



    /**
     * Adapted from cga
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

    @SubscribeEvent
    fun onWorld(event: WorldEvent.Unload){
        linesList.clear()
    }

}