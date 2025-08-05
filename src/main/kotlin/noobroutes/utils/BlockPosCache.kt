package noobroutes.utils

import net.minecraft.util.BlockPos

class BlockPosCache {
    private val cache: MutableSet<Int> = mutableSetOf();

    constructor() {

    }

    fun clear() {
        cache.clear();
    }

    fun add(blockPos: BlockPos) {

    }

    fun remove(blockPos: BlockPos) {

    }

    fun contains(blockPos: BlockPos) : Boolean {

    }

    private fun hash(blockPos: BlockPos) : Int {
        return blockPos
    }



}