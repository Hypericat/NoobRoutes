package noobroutes.utils

import net.minecraft.entity.Entity
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.network.play.server.S2APacketParticles
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraft.util.Vec3i
import noobroutes.Core.mc
import noobroutes.utils.render.RenderUtils.outlineBounds
import noobroutes.utils.skyblock.dungeon.tiles.Rotations
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

data class Vec2(val x: Double, val z: Double)

data class Vec2i(val x: Int, val z: Int)

data class LookVec(val yaw: Float, val pitch: Float)

data class PositionLook(val pos: Vec3, val yaw: Float, val pitch: Float)

fun BlockPos.add(vec: Vec2): BlockPos = this.add(vec.x, 0.0, vec.z)

fun BlockPos.add(vec: Vec2i): BlockPos = this.add(vec.x, 0, vec.z)


operator fun Vec3.component1(): Double = xCoord

operator fun Vec3.component2(): Double = yCoord

operator fun Vec3.component3(): Double = zCoord

operator fun Vec3i.component1(): Int = x

operator fun Vec3i.component2(): Int = y

operator fun Vec3i.component3(): Int = z

fun Entity.distanceSquaredTo(pos: Vec3): Double =
    (posX - pos.xCoord).pow(2.0) + (posY - pos.yCoord).pow(2.0) + (posZ - pos.zCoord).pow(2.0)

/**
 * Gets the distance between two entities, ignoring y coordinate, squared.
 */
fun Entity.xzDistance(entity1: Entity): Double =
    (posX - entity1.posX).pow(2.0) + (posZ - entity1.posZ).pow(2.0)

fun Vec3.add(x: Double, y: Double, z: Double): Vec3 {
    return this.add(Vec3(x, y, z))
}

val Vec3.length get() = sqrt(this.xCoord * this.xCoord + this.yCoord * this.yCoord + this.zCoord * this.zCoord)


fun Vec3.bloomNormalize(): Vec3 {
    val len =  1 / this.length
    return Vec3(this.xCoord * len, this.yCoord * len, this.zCoord * len)
}



/**
 * Gets the eye height of the player
 * @return The eye height of the player
 */
fun fastEyeHeight(): Float =
    if (mc.thePlayer?.isSneaking == true) 1.54f else 1.62f

/**
 * Gets the position of the player's eyes
 * @param pos The position to get the eyes of
 * @return The position of the player's eyes
 */
fun getPositionEyes(pos: Vec3 = mc.thePlayer?.positionVector ?: Vec3(0.0, 0.0, 0.0)): Vec3 = Vec3(pos.xCoord, pos.yCoord + 1.6200000047683716, pos.zCoord)

/**
 * Gets a normalized vector of the player's look
 * @param yaw The yaw of the player
 * @param pitch The pitch of the player
 * @return A normalized vector of the player's look
 */
fun getLook(yaw: Float = mc.thePlayer?.rotationYaw ?: 0f, pitch: Float = mc.thePlayer?.rotationPitch ?: 0f): Vec3 {
    val f2 = -cos(-pitch * 0.017453292f).toDouble()
    return Vec3(
        sin(-yaw * 0.017453292f - 3.1415927f) * f2,
        sin(-pitch * 0.017453292f).toDouble(),
        cos(-yaw * 0.017453292f - 3.1415927f) * f2
    )
}

/**
 * Returns true if the given position is being looked at by the player within the given range.
 * @param aabb The position to check
 * @param range The range to check
 * @param yaw The yaw of the player
 * @param pitch The pitch of the player
 * @return True if the given position is being looked at by the player within the given range
 */
fun isFacingAABB(aabb: AxisAlignedBB, range: Float, yaw: Float = mc.thePlayer?.rotationYaw ?: 0f, pitch: Float = mc.thePlayer?.rotationPitch ?: 0f): Boolean =
    isInterceptable(aabb, range, yaw, pitch)


/**
 * Returns true if the given position is being looked at by the player within the given range, ignoring the Y value.
 * @param aabb The position to check
 * @param range The range to check
 * @param yaw The yaw of the player
 * @param pitch The pitch of the player
 * @return True if the given position is being looked at by the player within the given range, ignoring the Y value
 */
fun isXZInterceptable(aabb: AxisAlignedBB, range: Float, pos: Vec3, yaw: Float, pitch: Float): Boolean = with(getPositionEyes(pos)) {
    return isXZInterceptable(this, this.add(getLook(yaw, pitch).multiply(range)), aabb)
}

private fun isXZInterceptable(start: Vec3, goal: Vec3?, aabb: AxisAlignedBB): Boolean {
    return  isVecInZ(start.getIntermediateWithXValue(goal, aabb.minX), aabb) ||
            isVecInZ(start.getIntermediateWithXValue(goal, aabb.maxX), aabb) ||
            isVecInX(start.getIntermediateWithZValue(goal, aabb.minZ), aabb) ||
            isVecInX(start.getIntermediateWithZValue(goal, aabb.maxZ), aabb)
}

/**
 * Multiplies every coordinate of a Vec3 by the given factor.
 * @param factor The factor to multiply byhow
 * @return The multiplied Vec3
 */
fun Vec3.multiply(factor: Number): Vec3 =
    Vec3(this.xCoord * factor.toDouble(), this.yCoord * factor.toDouble(), this.zCoord * factor.toDouble())

fun Vec3.multiply(x: Double = 1.0, y: Double = 1.0, z: Double = 1.0): Vec3 =
    Vec3(this.xCoord * x, this.yCoord * y, this.zCoord * z)

fun Vec3.equal(other: Vec3): Boolean =
    this.xCoord == other.xCoord && this.yCoord == other.yCoord && this.zCoord == other.zCoord

/**
 * Gets the coordinate of the given index of a Vec3. (0 = x, 1 = y, 2 = z)
 * @param index The index to get
 * @return The coordinate of the given index of a Vec3
 */
operator fun Vec3.get(index: Int): Double =
    when (index) {
        0 -> this.xCoord
        1 -> this.yCoord
        2 -> this.zCoord
        else -> throw IndexOutOfBoundsException("Index: $index, Size: 3")
    }

/**
 * Rotates a Vec3 around the given rotation.
 * @param rotation The rotation to rotate around
 * @return The rotated Vec3
 */
fun Vec3.rotateAroundNorth(rotation: Rotations): Vec3 =
    when (rotation) {
        Rotations.NORTH -> Vec3(-this.xCoord, this.yCoord, -this.zCoord)
        Rotations.WEST ->  Vec3(-this.zCoord, this.yCoord, this.xCoord)
        Rotations.SOUTH -> Vec3(this.xCoord, this.yCoord, this.zCoord)
        Rotations.EAST ->  Vec3(this.zCoord, this.yCoord, -this.xCoord)
        else -> this
    }

/**
 * Rotates a Vec3 to the given rotation.
 * @param rotation The rotation to rotate to
 * @return The rotated Vec3
 */
fun Vec3.rotateToNorth(rotation: Rotations): Vec3 =
    when (rotation) {
        Rotations.NORTH -> Vec3(-this.xCoord, this.yCoord, -this.zCoord)
        Rotations.WEST ->  Vec3(this.zCoord, this.yCoord, -this.xCoord)
        Rotations.SOUTH -> Vec3(this.xCoord, this.yCoord, this.zCoord)
        Rotations.EAST ->  Vec3(-this.zCoord, this.yCoord, this.xCoord)
        else -> this
    }

/**
 * Checks if an axis-aligned bounding box (AABB) is interceptable based on the player's position, range, yaw, and pitch.
 *
 * @param aabb The axis-aligned bounding box to check for interceptability.
 * @param range The range of the interception.
 * @param yaw The yaw angle.
 * @param pitch The pitch angle.
 * @return `true` if the AABB is interceptable, `false` otherwise.
 */
private fun isInterceptable(aabb: AxisAlignedBB, range: Float, yaw: Float, pitch: Float): Boolean = with(getPositionEyes()) {
    return isInterceptable3(this, this.add(getLook(yaw, pitch).multiply(range)), aabb)
}

/**
 * Checks if an axis-aligned bounding box (AABB) is interceptable between two points.
 *
 * @param start The starting point.
 * @param goal The ending point.
 * @param aabb The axis-aligned bounding box to check for interceptability.
 * @return `true` if the AABB is interceptable, `false` otherwise.
 */
private fun isInterceptable3(start: Vec3, goal: Vec3, aabb: AxisAlignedBB): Boolean {
    return try {
        (
            isVecInYZ(start.getIntermediateWithXValue(goal, aabb.minX), aabb) ||
            isVecInYZ(start.getIntermediateWithXValue(goal, aabb.maxX), aabb) ||
            isVecInXZ(start.getIntermediateWithYValue(goal, aabb.minY), aabb) ||
            isVecInXZ(start.getIntermediateWithYValue(goal, aabb.maxY), aabb) ||
            isVecInXY(start.getIntermediateWithZValue(goal, aabb.minZ), aabb) ||
            isVecInXY(start.getIntermediateWithZValue(goal, aabb.maxZ), aabb)
        )
    } catch (_: Exception) { false }
}

fun isVecInAABB(vec: Vec3, aabb: AxisAlignedBB): Boolean =
    vec.xCoord in aabb.minX..aabb.maxX && vec.yCoord in aabb.minY..aabb.maxY && vec.zCoord in aabb.minZ..aabb.maxZ

/**
 * Checks if a Vec3 is within the YZ bounds of an axis-aligned bounding box (AABB).
 *
 * @param vec The Vec3 to check.
 * @param aabb The axis-aligned bounding box.
 * @return `true` if the Vec3 is within the YZ bounds, `false` otherwise.
 */
private fun isVecInYZ(vec: Vec3, aabb: AxisAlignedBB): Boolean =
    vec.yCoord in aabb.minY..aabb.maxY && vec.zCoord in aabb.minZ..aabb.maxZ

/**
 * Checks if a Vec3 is within the XZ bounds of an axis-aligned bounding box (AABB).
 *
 * @param vec The Vec3 to check.
 * @param aabb The axis-aligned bounding box.
 * @return `true` if the Vec3 is within the XZ bounds, `false` otherwise.
 */
fun isVecInXZ(vec: Vec3, aabb: AxisAlignedBB): Boolean =
    vec.xCoord in aabb.minX..aabb.maxX && vec.zCoord in aabb.minZ..aabb.maxZ

/**
 * Checks if a Vec3 is within the XY bounds of an axis-aligned bounding box (AABB).
 *
 * @param vec The Vec3 to check.
 * @param aabb The axis-aligned bounding box.
 * @return `true` if the Vec3 is within the XY bounds, `false` otherwise.
 */
private fun isVecInXY(vec: Vec3, aabb: AxisAlignedBB): Boolean =
    vec.xCoord in aabb.minX..aabb.maxX && vec.yCoord in aabb.minY..aabb.maxY

/**
 * Checks if a Vec3 is within the Z bounds of an axis-aligned bounding box (AABB).
 *
 * @param vec The Vec3 to check.
 * @param aabb The axis-aligned bounding box.
 * @return `true` if the Vec3 is within the Z bounds, `false` otherwise.
 */
private fun isVecInZ(vec: Vec3?, aabb: AxisAlignedBB): Boolean =
    vec != null && vec.zCoord >= aabb.minZ && vec.zCoord <= aabb.maxZ

/**
 * Checks if a Vec3 is within the X bounds of an axis-aligned bounding box (AABB).
 *
 * @param vec The Vec3 to check.
 * @param aabb The axis-aligned bounding box.
 * @return `true` if the Vec3 is within the X bounds, `false` otherwise.
 */
private fun isVecInX(vec: Vec3?, aabb: AxisAlignedBB): Boolean =
    vec != null && vec.xCoord >= aabb.minX && vec.xCoord <= aabb.maxX

/**
 * Overloads the `plus` operator for Vec3 to provide vector addition.
 *
 * @param vec3 The Vec3 to add to the current Vec3.
 * @return A new Vec3 representing the sum of the two vectors.
 */
operator fun Vec3.plus(vec3: Vec3): Vec3 =
    this.add(vec3)

/**
 * Adds the given coordinates to the Vec3.
 */
fun Vec3.addVec(x: Number = .0, y: Number = .0, z: Number = .0): Vec3 =
    this.addVector(x.toDouble(), y.toDouble(), z.toDouble())

/**
 * Removes the given coordinates to the Vec3.
 */
fun Vec3.subtractVec(x: Number = .0, y: Number = .0, z: Number = .0): Vec3 =
    this.addVector(-x.toDouble(), -y.toDouble(), -z.toDouble())

/**
 * Adds the given coordinates to the Vec3.
 */
fun Vec3i.addVec(x: Number = .0, y: Number = .0, z: Number = .0): Vec3i =
    Vec3i(this.x + x.toInt(), this.y + y.toInt(), this.z + z.toInt())


/**
 * Floors every coordinate of a Vec3 and turns it into a Vec3i.
 */
fun Vec3.floored(): Vec3i =
    Vec3i(xCoord.floor(), yCoord.floor(), zCoord.floor())


/**
 * Floors every coordinate of a Vec3
 */
fun Vec3.flooredVec(): Vec3 =
    Vec3(xCoord.floor(), yCoord.floor(), zCoord.floor())

/**
 * @param add Will determine the maximum bounds
 */
fun BlockPos.toAABB(add: Double = 1.0): AxisAlignedBB =
    AxisAlignedBB(this.x.toDouble(), this.y.toDouble(), this.z.toDouble(), this.x + add, this.y + add, this.z + add).outlineBounds()

/**
 * @param add Will determine the maximum bounds
 */
fun Vec3.toAABB(add: Double = 1.0): AxisAlignedBB =
    AxisAlignedBB(this.xCoord, this.yCoord, this.zCoord, this.xCoord + add, this.yCoord + add, this.zCoord + add).outlineBounds()

/**
 * Turns a Vec3 into a BlockPos.
 */
fun Vec3.toBlockPos(add: Double = 0.0): BlockPos =
    BlockPos(this.xCoord + add, this.yCoord + add, this.zCoord + add)


/**
 * Clones a Vec3 object.
 */
fun Vec3.clone(): Vec3 =
    Vec3(this.xCoord, this.yCoord, this.zCoord)

/**
 * Turns a Vec3 into a double array.
 */
fun Vec3.toDoubleArray(): DoubleArray =
    doubleArrayOf(xCoord, yCoord, zCoord)

/**
 * Turns a BlockPos into a Vec3.
 */
fun BlockPos.toVec3(): Vec3 =
    Vec3(x.toDouble(), y.toDouble(), z.toDouble())

/**
 * Turns a BlockPos into a centered Vec3.
 */
fun BlockPos.toCenteredVec3(): Vec3 =
    Vec3(x.toDouble() + 0.5, y.toDouble(), z.toDouble() + 0.5)

/**
 * Turns a double array into a Vec3.
 */
fun DoubleArray.toVec3(): Vec3 =
    Vec3(this[0], this[1], this[2])

/**
 * Turns a BlockPos into a MutableVec3
 */
fun BlockPos.toMutableVec3(): MutableVec3 =
    MutableVec3(x.toDouble(), y.toDouble(), z.toDouble())

fun Vec3.toMutableVec3(): MutableVec3 =
    MutableVec3(xCoord, yCoord, zCoord)

/**
 * Solves the equation for diana burrow estimate.
 * @see noobroutes.utils.skyblock.DianaBurrowEstimate.guessPosition
 * @author Soopy
 */
fun calculateCoefficientsFromVectors(x: Vec3, y: Vec3): Triple<Double, Double, Double> {
    val a = (-y.xCoord * x.yCoord * x.xCoord - y.yCoord * x.yCoord * x.zCoord + y.yCoord * x.yCoord * x.xCoord + x.yCoord * x.zCoord * y.zCoord + x.xCoord * x.zCoord * y.xCoord - x.xCoord * x.zCoord * y.zCoord) / (x.yCoord * y.xCoord - x.yCoord * y.zCoord + x.xCoord * y.zCoord - y.xCoord * x.zCoord + y.yCoord * x.zCoord - y.yCoord * x.xCoord)
    val b = (y.xCoord - y.yCoord) * (x.xCoord + a) * (x.yCoord + a) / (x.yCoord - x.xCoord)
    val c = y.xCoord - b / (x.xCoord + a)
    return Triple(a, b, c)
}

/**
 * Coerces the given Vec3's y coordinate to be within the given range.
 */
fun Vec3.coerceYIn(min: Double, max: Double): Vec3 =
    Vec3(xCoord, yCoord.coerceIn(min, max), zCoord)


/**
 * Gets the Vec3 position of the given S29PacketSoundEffect.
 * @author Bonsai
 */
val S29PacketSoundEffect.positionVector: Vec3 get() =
    Vec3(this.x, this.y, this.z)

val S2APacketParticles.positionVector: Vec3 get() =
    Vec3(this.xCoordinate, this.yCoordinate, this.zCoordinate)

val AxisAlignedBB.corners: List<Vec3> get() =
    listOf(
        Vec3(minX, minY, minZ), Vec3(minX, maxY, minZ), Vec3(maxX, maxY, minZ), Vec3(maxX, minY, minZ),
        Vec3(minX, minY, maxZ), Vec3(minX, maxY, maxZ), Vec3(maxX, maxY, maxZ), Vec3(maxX, minY, maxZ)
    )

operator fun Vec3.unaryMinus(): Vec3 =
    Vec3(-xCoord, -yCoord, -zCoord)

fun AxisAlignedBB.offset(vec: Vec3) =
    AxisAlignedBB(this.minX + vec.xCoord, this.minY + vec.yCoord, this.minZ + vec.zCoord, this.maxX + vec.xCoord, this.maxY + vec.yCoord, this.maxZ + vec.zCoord)

val AxisAlignedBB.middle: Vec3 get() =
    Vec3(this.minX + (this.maxX - this.minX) / 2, this.minY + (this.maxY - this.minY) / 2, this.minZ + (this.maxZ - this.minZ) / 2)



fun wrapAngle(angle: Float): Float {
    var newAngle = angle
    while (newAngle >= 180f) newAngle -= 360f
    while (newAngle < -180f) newAngle += 360f
    return newAngle
}

fun bezier(t: Float, initial: Float, p1: Float, p2: Float, final: Float): Float =
    (1 - t).pow(3) * initial + 3 * (1 - t).pow(2) * t * p1 + 3 * (1 - t) * t.pow(2) * p2 + t.pow(3) * final

fun BlockPos.addRotationCoords(rotation: noobroutes.utils.skyblock.dungeon.tiles.Rotations, x: Int, z: Int): BlockPos =
    when (rotation) {
        Rotations.NORTH -> BlockPos(this.x + x, this.y, this.z + z)
        Rotations.SOUTH -> BlockPos(this.x - x, this.y, this.z - z)
        Rotations.WEST ->  BlockPos(this.x + z, this.y, this.z - x)
        Rotations.EAST ->  BlockPos(this.x - z, this.y, this.z + x)
        else -> this
    }