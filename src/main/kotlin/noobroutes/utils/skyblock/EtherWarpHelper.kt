package noobroutes.utils.skyblock

import noobroutes.Core.mc
import noobroutes.utils.*
import noobroutes.utils.render.RenderUtils.renderVec
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import kotlin.math.*

object EtherWarpHelper {
    data class EtherPos(val succeeded: Boolean, val pos: BlockPos?) {
        val vec: Vec3? get() = pos?.let { Vec3(it) }
        companion object {
            val NONE = EtherPos(false, null)
        }
    }
    var etherPos: EtherPos = EtherPos.NONE

    /**
     * Gets the position of an entity in the "ether" based on the player's view direction.
     *
     * @param pos The initial position of the entity.
     * @param yaw The yaw angle representing the player's horizontal viewing direction.
     * @param pitch The pitch angle representing the player's vertical viewing direction.
     * @return An `EtherPos` representing the calculated position in the "ether" or `EtherPos.NONE` if the player is not present.
     */
    fun getEtherPos(pos: Vec3, yaw: Float, pitch: Float, distance: Double = 60.0, returnEnd: Boolean = false): EtherPos {
        mc.thePlayer ?: return EtherPos.NONE

        val startPos: Vec3 = getPositionEyes(pos)
        val endPos = getLook(yaw = yaw, pitch = pitch).normalize().multiply(factor = distance).add(startPos)

        return traverseVoxels(startPos, endPos).takeUnless { it == EtherPos.NONE && returnEnd } ?: EtherPos(true, endPos.toBlockPos())
    }

    fun getEtherPos(positionLook: PositionLook = PositionLook(
        mc.thePlayer.renderVec,
        mc.thePlayer.rotationYaw,
        mc.thePlayer.rotationPitch
    ), distance: Double = 60.0): EtherPos {
        return getEtherPos(positionLook.pos, positionLook.yaw, positionLook.pitch, distance)
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


    /**
     * DOES NOT WORK
     * taken from MeowClient
     *
     * @param {*} maxDistance
     * @param {*} partialTicks
     * @param {*} forceSneak
     * @param {*} yaw
     * @param {*} pitch
     * @returns [x, y, z] | null
     */
    fun rayTraceBlock(maxDistance: Int = 50, partialTicks: Float = 1f, forceSneak: Boolean = false, yaw: Float = mc.thePlayer.rotationYaw, pitch: Float = mc.thePlayer.rotationPitch, renderVec: Vec3 = mc.thePlayer.renderVec): Vec3? {

        var yaw = yaw.round(14).toDouble()
        var pitch = pitch.round(14).toDouble()

        yaw *= Math.PI / 180f
        pitch *= Math.PI / 180f


        val cosPitch = cos(pitch);

        val dx = -cosPitch * sin(yaw);
        val dy = -sin(pitch);
        val dz = cosPitch * cos(yaw);

        var x = floor(renderVec.xCoord);
        var y = floor(renderVec.yCoord);
        var z = floor(renderVec.zCoord);

        val stepX = if (dx < 0) -1 else 1;
        val stepY = if (dy < 0) -1 else 1;
        val stepZ = if (dz < 0) -1 else 1;

        val tDeltaX = abs(1 / dx);
        val tDeltaY = abs(1 / dy);
        val tDeltaZ = abs(1 / dz);

        var tMaxX = (if (dx < 0) renderVec.xCoord - x else x + 1 - renderVec.xCoord) * tDeltaX;
        var tMaxY = (if (dy < 0) renderVec.yCoord - y else y + 1 - renderVec.yCoord) * tDeltaY;
        var tMaxZ = (if (dz < 0) renderVec.zCoord - z else z + 1 - renderVec.zCoord) * tDeltaZ;

        if (!isAir(BlockPos(x, y, z))) {
            return Vec3(renderVec.xCoord, renderVec.yCoord, renderVec.zCoord)
        }

        for (i in 0 until maxDistance) {

            val c = minOf(tMaxX, tMaxY, tMaxZ);

            val hit = listOf<Double>(
                renderVec.xCoord + dx * c,
                renderVec.yCoord + dy * c,
                renderVec.zCoord + dz * c
            ).map {
                coord -> (coord * 1e10).roundToInt() * 1e-10
            }

            if (tMaxX < tMaxY && tMaxX < tMaxZ) {
                x += stepX;
                tMaxX += tDeltaX;
            } else if (tMaxY < tMaxZ) {
                y += stepY;
                tMaxY += tDeltaY;
            } else {
                z += stepZ;
                tMaxZ += tDeltaZ;
            }

            if (!isAir(BlockPos(x,y,z))) {
                return Vec3(hit[0], hit[1], hit[2])
            }
        }

        return null;
    }


    /**
     * Checks if the block at the given position is a valid block to etherwarp onto.
     * @author Bloom
     */
    private fun isValidEtherWarpBlock(pos: BlockPos): Boolean {
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