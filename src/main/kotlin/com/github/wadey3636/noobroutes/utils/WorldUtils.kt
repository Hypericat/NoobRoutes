package com.github.wadey3636.noobroutes.utils

import me.defnotstolen.Core.mc
import net.minecraft.block.Block
import net.minecraft.block.BlockButton
import net.minecraft.block.BlockLever
import net.minecraft.block.BlockLever.EnumOrientation
import net.minecraft.block.BlockSkull
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.*


/**
 * Retrieves the block ID at the specified `BlockPos` in the Minecraft world.
 *
 * @param blockPos The position in the world to query for the block ID.
 * @return The block ID as an `Int`, or `null` if the block at the given position is not present.
 */
fun getBlockIdAt(blockPos: BlockPos): Int? {
    return Block.getIdFromBlock(getBlockStateAt(blockPos).block ?: return null)
}

/**
 * Checks if the block at the specified `BlockPos` is considered "air" in the Minecraft world.
 *
 * @param blockPos The position in the world to query.
 * @return `true` if the block at the given position is air, `false` otherwise.
 */
fun isAir(blockPos: BlockPos): Boolean =
    getBlockAt(blockPos) == Blocks.air

fun getLookingBlock(){

}


/**
 * Checks if the block at the specified `BlockPos` is a gold block in the Minecraft world.
 *
 * @param blockPos The position in the world to query.
 * @return `true` if the block at the given position is a gold block, `false` otherwise.
 */
fun isGold(blockPos: BlockPos): Boolean =
    getBlockAt(blockPos) == Blocks.gold_block

/**
 * Retrieves the block at the specified `BlockPos` in the Minecraft world.
 *
 * @param pos The position in the world to query for the block.
 * @return The block at the given position, or `Blocks.air` if the block is not present.
 */
fun getBlockAt(pos: BlockPos): Block =
    mc.theWorld?.chunkProvider?.provideChunk(pos.x shr 4, pos.z shr 4)?.getBlock(pos) ?: Blocks.air

/**
 * Retrieves the block state at the specified `BlockPos` in the Minecraft world.
 *
 * @param pos The position in the world to query for the block state.
 * @return The block state at the given position, or the default state of `Blocks.air` if the block is not present.
 */
fun getBlockStateAt(pos: BlockPos): IBlockState =
    mc.theWorld?.chunkProvider?.provideChunk(pos.x shr 4, pos.z shr 4)?.getBlockState(pos) ?: Blocks.air.defaultState

object BlockUtils {
    fun collisionRayTrace(pos: BlockPos?, aabb: AxisAlignedBB, start: Vec3, end: Vec3): MovingObjectPosition? {
        var start = start
        var end = end
        start = start.subtract(Vec3(pos))
        end = end.subtract(Vec3(pos))
        var vec3 = start.getIntermediateWithXValue(end, aabb.minX)
        var vec31 = start.getIntermediateWithXValue(end, aabb.maxX)
        var vec32 = start.getIntermediateWithYValue(end, aabb.minY)
        var vec33 = start.getIntermediateWithYValue(end, aabb.maxY)
        var vec34 = start.getIntermediateWithZValue(end, aabb.minZ)
        var vec35 = start.getIntermediateWithZValue(end, aabb.maxZ)

        if (isVecOutsideYZBounds(vec3, aabb.minY, aabb.minZ, aabb.maxY, aabb.maxZ)) vec3 = null
        if (isVecOutsideYZBounds(vec31, aabb.minY, aabb.minZ, aabb.maxY, aabb.maxZ)) vec31 = null
        if (isVecOutsideXZBounds(vec32, aabb.minX, aabb.minZ, aabb.maxX, aabb.maxZ)) vec32 = null
        if (isVecOutsideXZBounds(vec33, aabb.minX, aabb.minZ, aabb.maxX, aabb.maxZ)) vec33 = null
        if (isVecOutsideXYBounds(vec34, aabb.minX, aabb.minY, aabb.maxX, aabb.maxY)) vec34 = null
        if (isVecOutsideXYBounds(vec35, aabb.minX, aabb.minY, aabb.maxX, aabb.maxY)) vec35 = null

        var vec36: Vec3? = null
        if (vec3 != null) vec36 = vec3
        if (vec31 != null && (vec36 == null || start.squareDistanceTo(vec31) < start.squareDistanceTo(vec36))) vec36 =
            vec31
        if (vec32 != null && (vec36 == null || start.squareDistanceTo(vec32) < start.squareDistanceTo(vec36))) vec36 =
            vec32
        if (vec33 != null && (vec36 == null || start.squareDistanceTo(vec33) < start.squareDistanceTo(vec36))) vec36 =
            vec33
        if (vec34 != null && (vec36 == null || start.squareDistanceTo(vec34) < start.squareDistanceTo(vec36))) vec36 =
            vec34
        if (vec35 != null && (vec36 == null || start.squareDistanceTo(vec35) < start.squareDistanceTo(vec36))) vec36 =
            vec35

        if (vec36 == null) return null
        else {
            var enumfacing: EnumFacing? = null
            if (vec36 === vec3) enumfacing = EnumFacing.WEST
            if (vec36 === vec31) enumfacing = EnumFacing.EAST
            if (vec36 === vec32) enumfacing = EnumFacing.DOWN
            if (vec36 === vec33) enumfacing = EnumFacing.UP
            if (vec36 === vec34) enumfacing = EnumFacing.NORTH
            if (vec36 === vec35) enumfacing = EnumFacing.SOUTH
            return MovingObjectPosition(vec36, enumfacing, pos)
        }
    }

    private fun isVecOutsideYZBounds(point: Vec3?, minY: Double, minZ: Double, maxY: Double, maxZ: Double): Boolean {
        return point == null || !(point.yCoord >= minY) || !(point.yCoord <= maxY) || !(point.zCoord >= minZ) || !(point.zCoord <= maxZ)
    }

    private fun isVecOutsideXZBounds(point: Vec3?, minX: Double, minZ: Double, maxX: Double, maxZ: Double): Boolean {
        return point == null || !(point.xCoord >= minX) || !(point.xCoord <= maxX) || !(point.zCoord >= minZ) || !(point.zCoord <= maxZ)
    }

    private fun isVecOutsideXYBounds(point: Vec3?, minX: Double, minY: Double, maxX: Double, maxY: Double): Boolean {
        return point == null || !(point.xCoord >= minX) || !(point.xCoord <= maxX) || !(point.yCoord >= minY) || !(point.yCoord <= maxY)
    }

    fun getAABB(block: BlockPos): AxisAlignedBB? {
        val blockState = mc.theWorld.getBlockState(block)
        return when (blockState.block) {
            Blocks.chest -> AxisAlignedBB(0.0625, 0.0, 0.0625, 0.9375, 0.875, 0.9375)
            Blocks.trapped_chest -> AxisAlignedBB(0.0625, 0.0, 0.0625, 0.9375, 0.875, 0.9375)
            Blocks.ender_chest -> AxisAlignedBB(0.0625, 0.0, 0.0625, 0.9375, 0.875, 0.9375)
            Blocks.lever -> {
                val orientation = blockState.properties[BlockLever.FACING] as EnumOrientation
                when(orientation) {
                    EnumOrientation.EAST -> AxisAlignedBB(0.0, 0.2, 0.315, 0.375, 0.8, 0.6875)
                    EnumOrientation.WEST -> AxisAlignedBB(0.625, 0.2, 0.315, 1.0, 0.8, 0.6875)
                    EnumOrientation.SOUTH -> AxisAlignedBB(0.3125, 0.2, 0.0, 0.6875, 0.8, 0.375)
                    EnumOrientation.NORTH -> AxisAlignedBB(0.3125, 0.2, 0.625, 0.6875, 0.8, 1.0)
                    EnumOrientation.UP_Z, EnumOrientation.UP_X -> AxisAlignedBB(0.25, 0.0, 0.25, 0.75, 0.6, 0.75)
                    EnumOrientation.DOWN_X, EnumOrientation.DOWN_Z -> AxisAlignedBB(0.25, 0.4, 0.25, 0.75, 1.0, 0.75)
                }
            }
            Blocks.skull -> {
                when (blockState.properties[BlockSkull.FACING] as EnumFacing) {
                    EnumFacing.NORTH -> AxisAlignedBB(0.25, 0.25, 0.5, 0.75, 0.75, 1.0)
                    EnumFacing.SOUTH -> AxisAlignedBB(0.25, 0.25, 0.0, 0.75, 0.75, 0.5)
                    EnumFacing.WEST -> AxisAlignedBB(0.5, 0.25, 0.25, 1.0, 0.75, 0.75)
                    EnumFacing.EAST -> AxisAlignedBB(0.0, 0.25, 0.25, 0.5, 0.75, 0.75)
                    else -> AxisAlignedBB(0.25, 0.0, 0.25, 0.75, 0.5, 0.75)
                }

            }
            Blocks.redstone_block -> AxisAlignedBB(0.0,0.0,0.0, 1.0,1.0,1.0)
            Blocks.stone_button  -> {
                val enumfacing = blockState.getValue(BlockButton.FACING) as EnumFacing
                val flag = blockState.getValue(BlockButton.POWERED) as Boolean
                val f2 = (if (flag) 1 else 2).toDouble() / 16.0
                when (enumfacing) {
                    EnumFacing.EAST -> AxisAlignedBB(0.0, 0.375, 0.3125, f2, 0.625, 0.6875)
                    EnumFacing.WEST -> AxisAlignedBB(1.0 - f2, 0.375, 0.3125, 1.0, 0.625, 0.6875)
                    EnumFacing.SOUTH -> AxisAlignedBB(0.3125, 0.375, 0.0, 0.6875, 0.625, f2)
                    EnumFacing.NORTH -> AxisAlignedBB(0.3125, 0.375, 1.0f - f2, 0.6875, 0.625, 1.0)
                    EnumFacing.UP -> AxisAlignedBB(0.3125, 0.0, 0.375, 0.6875, 0.0f + f2, 0.625)
                    EnumFacing.DOWN -> AxisAlignedBB(0.3125, 1.0f - f2, 0.375, 0.6875, 1.0, 0.625)
                }
            }
            else -> null
        }
    }
/*
    fun clickLever(lever: BlockPos) {
        val orientation = mc.theWorld.getBlockState(lever).properties[BlockLever.FACING] as EnumOrientation
        val aabb = when(orientation) {
            EnumOrientation.EAST -> AxisAlignedBB(0.0, 0.2, 0.315, 0.375, 0.8, 0.6875)
            EnumOrientation.WEST -> AxisAlignedBB(0.625, 0.2, 0.315, 1.0, 0.8, 0.6875)
            EnumOrientation.SOUTH -> AxisAlignedBB(0.3125, 0.2, 0.0, 0.6875, 0.8, 0.375)
            EnumOrientation.NORTH -> AxisAlignedBB(0.3125, 0.2, 0.625, 0.6875, 0.8, 1.0)
            EnumOrientation.UP_Z, EnumOrientation.UP_X -> AxisAlignedBB(0.25, 0.0, 0.25, 0.75, 0.6, 0.75)
            EnumOrientation.DOWN_X, EnumOrientation.DOWN_Z -> AxisAlignedBB(0.25, 0.4, 0.25, 0.75, 1.0, 0.75)
            else -> return
        }

        val centerPos = Vec3(lever).addVector(
            (aabb.minX + aabb.maxX) / 2,
            (aabb.minY + aabb.maxY) / 2,
            (aabb.minZ + aabb.maxZ) / 2
        )

        val movingObjectPosition: MovingObjectPosition = collisionRayTrace(lever, aabb, mc.thePlayer.getPositionEyes(0f), centerPos) ?: return
        PacketUtils.sendPacket(
            C08PacketPlayerBlockPlacement(
                lever,
                movingObjectPosition.sideHit.index,
                mc.thePlayer.heldItem,
                movingObjectPosition.hitVec.xCoord.toFloat(),
                movingObjectPosition.hitVec.yCoord.toFloat(),
                movingObjectPosition.hitVec.zCoord.toFloat()
            )
        )
    }

 */
}