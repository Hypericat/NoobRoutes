package noobroutes.features.dungeon.brush

import net.minecraft.block.Block
import net.minecraft.block.properties.IProperty
import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos
import noobroutes.Core.logger
import noobroutes.Core.mc
import noobroutes.config.DataManager
import noobroutes.utils.IBlockStateUtils
import noobroutes.utils.json.JsonUtils.asBlockPos



object FmeConfigLoader {

    fun loadConfig() {
        try {
            val floorConfig = DataManager.loadDataFromFileObjectOfObjects("floorsConfig")
            floorConfig.forEach {
                val areaName = if (it.key.toIntOrNull() != null) "Floor ${it.key}" else it.key
                val obj = it.value.asJsonObject
                val areaBlocks = mutableListOf<Pair<IBlockState, BlockPos>>()
                obj.entrySet().forEach { blockList ->
                    val state = getBlockFromFME(blockList.key)
                    blockList.value.asJsonArray.forEach { block ->
                        areaBlocks.add(Pair(state, block.asBlockPos))
                    }
                }
                Brush.blockConfig[areaName] = areaBlocks
            }
        } catch (e: Exception) {
            logger.error("error loading config", e)
        }
    }


    fun getBlockFromFME(raw: String): IBlockState {
        val hasArgs = raw.contains("[") && raw.contains("]")

        if (!hasArgs) {
            val block = Block.getBlockFromName(raw)
            return block?.defaultState ?: IBlockStateUtils.airIBlockState
        }
        val baseName = raw.substringBefore("[")
        val block = Block.getBlockFromName(baseName)
        var state: IBlockState = block?.defaultState ?: IBlockStateUtils.airIBlockState

        val inner = raw.substringAfter("[").substringBefore("]")
        val args = inner.split(",")

        for (arg in args) {
            val parts = arg.split("=", limit = 2)
            if (parts.size != 2) continue
            val key = parts[0]
            val value = parts[1]
            val prop = state.propertyNames
                .firstOrNull { it.name == key } as? IProperty<*>
                ?: continue

            state = IBlockStateUtils.withProperty(state, prop, value)
        }

        return state
    }
}
