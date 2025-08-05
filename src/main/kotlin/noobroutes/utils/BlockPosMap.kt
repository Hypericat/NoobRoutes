package noobroutes.utils

import net.minecraft.util.BlockPos
import org.lwjgl.Sys
import kotlin.random.Random

class BlockPosMap<V> {
    private val cache: MutableMap<Int, V> = mutableMapOf()

    fun clear() {
        cache.clear();
    }

    operator fun get(key: BlockPos): V? {
        return cache[hash(key)]
    }

    fun put(key: BlockPos, value: V) {
        cache.put(hash(key), value)
    }

    fun remove(blockPos: BlockPos) {
        cache.remove(hash(blockPos))
    }

    operator fun set(key: BlockPos, value: V) {
        cache.put(hash(key), value)
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