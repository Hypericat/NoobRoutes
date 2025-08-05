package noobroutes.utils

import net.minecraft.util.BlockPos
import org.lwjgl.Sys
import kotlin.random.Random

class BlockPosCache {
    private val cache: MutableSet<Int> = mutableSetOf();

    fun clear() {
        cache.clear();
    }

    fun add(blockPos: BlockPos) {
        cache.add(hash(blockPos))
    }

    fun remove(blockPos: BlockPos) {
        cache.remove(hash(blockPos))
    }

    fun contains(blockPos: BlockPos) : Boolean {
        return cache.contains(hash(blockPos))
    }

    // Don't fucking put invalid values you monkeys
    // Y range : 0 to 255
    // X range: -2048 to 2047
    // Z range: -2048 to 2047
    private fun hash(blockPos: BlockPos) : Int {
        return blockPos.y and 0xFF or (((blockPos.x + 2048) and 0xFFF) shl 8) or (((blockPos.z + 2048) and 0xFFF) shl 20);
    }


}