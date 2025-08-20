package noobroutes.utils.skyblock

import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.utils.*
import noobroutes.utils.render.RenderUtils.renderVec
import noobroutes.utils.skyblock.PlayerUtils.SNEAK_EYE_HEIGHT
import kotlin.math.*

/**
 * Taken from odin
 */
object EtherWarpHelper {
    data class EtherPos(val succeeded: Boolean, val pos: BlockPos?) {
        val vec: Vec3? get() = pos?.let { Vec3(it) }
        companion object {
            val NONE = EtherPos(false, null)
        }
    }

    fun getEtherYawPitch(blockCoords: BlockPos, startCoords: Vec3): Pair<Float, Float>? {
        val centeredCoords = centerCoords(blockCoords)
        val rotation = RotationUtils.getYawAndPitch(
            centeredCoords.xCoord,
            centeredCoords.yCoord + 0.5,
            centeredCoords.zCoord
        )

        if (getEtherPosOrigin(startCoords, rotation.first, rotation.second).pos == blockCoords) {
            return rotation
        }
        var runs = 0
        val distance = startCoords.add(0.0, SNEAK_EYE_HEIGHT, 0.0).distanceTo(centeredCoords)
        val sweepDegrees = Math.toDegrees(2 * atan(0.707 / distance)).toFloat()
        for (i in 0..10) {
            val lowerYaw = rotation.first - sweepDegrees
            val upperYaw = rotation.first + sweepDegrees
            val lowerPitch = rotation.second - sweepDegrees
            val upperPitch = rotation.second + sweepDegrees

            val yawStepSize = (1.0 / (1 + i * (2.0 / 3))).toFloat()
            val pitchStepSize = (0.5 / (1 + i * 0.5)).toFloat()

            var yaw = lowerYaw
            while (yaw < upperYaw) {
                var pitch = lowerPitch
                while (pitch < upperPitch) {
                    runs++
                    val prediction = getEtherPosOrigin(startCoords, yaw, pitch)
                    if (prediction.pos == blockCoords) {
                        return Pair(yaw, pitch)
                    }
                    pitch += pitchStepSize
                }
                yaw += yawStepSize
            }
        }
        return null
    }

    /**
     * Gets the position of an entity in the "ether" based on the origin's view direction.
     *
     * @param origin The initial position of the entity.
     * @param yaw The yaw angle representing the player's horizontal viewing direction.
     * @param pitch The pitch angle representing the player's vertical viewing direction.
     * @return An `EtherPos` representing the calculated position in the "ether" or `EtherPos.NONE` if the player is not present.
     */
    fun getEtherPosFromOrigin(origin: Vec3, yaw: Float, pitch: Float, distance: Double = 60.0, returnEnd: Boolean = false): EtherPos {
        mc.thePlayer ?: return EtherPos.NONE
        val endPos = getLook(yaw = yaw, pitch = pitch).normalize().multiply(factor = distance).add(origin)
        return traverseVoxels(origin, endPos).takeUnless { it == EtherPos.NONE && returnEnd } ?: EtherPos(true, endPos.toBlockPos())
    }



    /**
     * Gets the position of an entity in the "ether" based on the player's view direction.
     *
     * @param pos The initial position of the entity.
     * @param yaw The yaw angle representing the player's horizontal viewing direction.
     * @param pitch The pitch angle representing the player's vertical viewing direction.
     * @return An `EtherPos` representing the calculated position in the "ether" or `EtherPos.NONE` if the player is not present.
     */
    fun getEtherPosOrigin(pos: Vec3, yaw: Float, pitch: Float, distance: Double = 61.0, returnEnd: Boolean = false, sneaking: Boolean = mc.thePlayer.isSneaking): EtherPos {
        mc.thePlayer ?: return EtherPos.NONE

        val startPos: Vec3 = getPositionEyes(pos, sneaking)
        val endPos = getLook(yaw = yaw, pitch = pitch).normalize().multiply(factor = distance).add(startPos)

        return traverseVoxels(startPos, endPos).takeUnless { it == EtherPos.NONE && returnEnd } ?: EtherPos(true, endPos.toBlockPos())
    }

    fun getEtherPosOrigin(positionLook: PositionLook = PositionLook(
        mc.thePlayer.renderVec,
        mc.thePlayer.rotationYaw,
        mc.thePlayer.rotationPitch
    ), distance: Double = 60.0): EtherPos {
        return getEtherPosOrigin(positionLook.pos, positionLook.yaw, positionLook.pitch, distance)
    }

    fun centerCoords(blockCoords: BlockPos): Vec3 {
        return Vec3(blockCoords.x + 0.5, blockCoords.y.toDouble(), blockCoords.z + 0.5)
    }

    /**
     * Traverses voxels from start to end and returns the first non-air block it hits.
     * @author Bloom
     */
    private fun traverseVoxels(start: Vec3, end: Vec3): EtherPos {
        val direction = end.subtract(start)
        val step = IntArray(3) { sign(direction[it]).toInt() }
        val invDirection = DoubleArray(3) { if (direction[it] != 0.0) 1.0 / direction[it] else Double.MAX_VALUE }
        val tDelta = DoubleArray(3) { invDirection[it] * step[it] }
        val currentPos = IntArray(3) { floor(start[it]).toInt() }
        val endPos = IntArray(3) { floor(end[it]).toInt() }
        val tMax = DoubleArray(3) {
            val startCoord = start[it]
            abs((floor(startCoord) + max(step[it], 0) - startCoord) * invDirection[it])
        }

        repeat(1000) {
            val pos = BlockPos(currentPos[0], currentPos[1], currentPos[2])
            if (getBlockIdAt(pos) != 0) return EtherPos(isValidEtherWarpBlock(pos), pos)
            if (currentPos.contentEquals(endPos)) return EtherPos.NONE

            val minIndex = if (tMax[0] <= tMax[1])
                if (tMax[0] <= tMax[2]) 0 else 2
            else
                if (tMax[1] <= tMax[2]) 1 else 2

            tMax[minIndex] += tDelta[minIndex]
            currentPos[minIndex] += step[minIndex]
        }

        return EtherPos.NONE
    }
    const val DEGREES_TO_RADIAN = Math.PI / 180;
    /**
     * taken from MeowClient
     *
     * @param {*} maxDistance
     * @param {*} partialTicks
     * @param {*} forceSneak
     * @param {*} yaw
     * @param {*} pitch
     * @returns [x, y, z] | null
     */
    fun rayTraceBlock(
        maxDistance: Int = 50,
        partialTicks: Float = 1f,
        yaw: Float = mc.thePlayer.rotationYaw,
        pitch: Float = mc.thePlayer.rotationPitch,
        playerX: Double? = null, playerY: Double? = null, playerZ: Double? = null
    ): Vec3? {
        val eyeX = playerX ?: (mc.thePlayer.lastTickPosX + (mc.thePlayer.posX - mc.thePlayer.lastTickPosX) * partialTicks)
        val eyeY = playerY
            ?: ((mc.thePlayer.lastTickPosY + (mc.thePlayer.posY - mc.thePlayer.lastTickPosY) * partialTicks) + SNEAK_EYE_HEIGHT)
        val eyeZ = playerZ ?: (mc.thePlayer.lastTickPosZ + (mc.thePlayer.posZ - mc.thePlayer.lastTickPosZ) * partialTicks)

        val roundedYaw = (yaw.round(14) * DEGREES_TO_RADIAN).toDouble()
        val roundedPitch = (pitch.round(14) * DEGREES_TO_RADIAN).toDouble()
        val cosPitch = cos(roundedPitch)
        val dx = -cosPitch * sin(roundedYaw)
        val dy = -sin(roundedPitch)
        val dz = cosPitch * cos(roundedYaw)

        var x = floor(eyeX)
        var y = floor(eyeY)
        var z = floor(eyeZ)

        val stepX = if (dx < 0) -1 else 1
        val stepY = if (dy < 0) -1 else 1
        val stepZ = if (dz < 0) -1 else 1

        val tDeltaX = abs(1 / dx)
        val tDeltaY = abs(1 / dy)
        val tDeltaZ = abs(1 / dz)

        var tMaxX = (if (dx < 0) eyeX - x else x + 1 - eyeX) * tDeltaX
        var tMaxY = (if (dy < 0) eyeY - y else y + 1 - eyeY) * tDeltaY
        var tMaxZ = (if (dz < 0) eyeZ - z else z + 1 - eyeZ) * tDeltaZ

        if (!isAir(BlockPos(x, y, z))) return Vec3(eyeX, eyeY, eyeZ)
        var i = 0
        while (i < maxDistance) {
            i++
            val c = minOf(tMaxX, tMaxY, tMaxZ)
            val hit = listOf(
                eyeX + dx * c,
                eyeY + dy * c,
                eyeZ + dz * c
            ).map { coord ->
                round(coord * 1e10) * 1e-10
            }
            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                x += stepX
                tMaxX += tDeltaX
            } else if (tMaxY < tMaxZ) {
                y += stepY
                tMaxY += tDeltaY
            } else {
                z += stepZ
                tMaxZ += tDeltaZ
            }
            if (!isAir(BlockPos(x, y, z))) return Vec3(hit[0], hit[1], hit[2])
        }
        return null
    }



    /**
     * Checks if the block at the given position is a valid block to etherwarp onto.
     * @author Bloom
     */
    fun isValidEtherWarpBlock(pos: BlockPos): Boolean {
        // Checking the actual block to etherwarp ontop of
        // Can be at foot level, but not etherwarped onto directly.
        if (getBlockAt(pos).registryName in validEtherwarpFeetBlocks || getBlockAt(pos.up(1)).registryName !in validEtherwarpFeetBlocks) return false

        return getBlockAt(pos.up(2)).registryName in validEtherwarpFeetBlocks
    }

    private val validEtherwarpFeetBlocks = setOf(
        "minecraft:air",
        "minecraft:fire",
        "minecraft:carpet",
        "minecraft:skull",
        "minecraft:lever",
        "minecraft:stone_button",
        "minecraft:wooden_button",
        "minecraft:torch",
        "minecraft:string",
        "minecraft:tripwire_hook",
        "minecraft:tripwire",
        "minecraft:rail",
        "minecraft:activator_rail",
        "minecraft:snow_layer",
        "minecraft:carrots",
        "minecraft:wheat",
        "minecraft:potatoes",
        "minecraft:nether_wart",
        "minecraft:pumpkin_stem",
        "minecraft:melon_stem",
        "minecraft:redstone_torch",
        "minecraft:redstone_wire",
        "minecraft:red_flower",
        "minecraft:yellow_flower",
        "minecraft:sapling",
        "minecraft:flower_pot",
        "minecraft:deadbush",
        "minecraft:tallgrass",
        "minecraft:ladder",
        "minecraft:double_plant",
        "minecraft:unpowered_repeater",
        "minecraft:powered_repeater",
        "minecraft:unpowered_comparator",
        "minecraft:powered_comparator",
        "minecraft:web",
        "minecraft:waterlily",
        "minecraft:water",
        "minecraft:lava",
        "minecraft:torch",
        "minecraft:vine",
    )
}