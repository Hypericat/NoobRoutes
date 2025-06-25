package noobroutes.features.dungeon.brush

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonIOException
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import net.minecraft.block.Block
import net.minecraft.block.properties.IProperty
import net.minecraft.block.state.IBlockState
import net.minecraft.server.management.PlayerProfileCache.dateFormat
import net.minecraft.util.BlockPos
import noobroutes.Core.logger
import noobroutes.Core.mc
import noobroutes.utils.IBlockStateUtils
import java.io.File
import java.io.FileInputStream
import java.lang.reflect.Type
import java.nio.charset.Charset


object FmeConfigLoader {

    val path = File(mc.mcDataDir, "config/noobroutes/")
    private fun Map<String, Map<*, Set<BlockPos>>>.toBlockConfig():
            MutableMap<String, MutableList<Pair<IBlockState, BlockPos>>>
    {
        val out = mutableMapOf<String, MutableList<Pair<IBlockState, BlockPos>>>()

        forEach { (area, rawStateMap) ->
            val list = mutableListOf<Pair<IBlockState, BlockPos>>()

            rawStateMap.forEach { (rawKey, positions) ->
                // 1) figure out the real IBlockState from the raw map-key:
                val state: IBlockState = when (rawKey) {
                    is IBlockState -> rawKey
                    is String -> {
                        // split "minecraft:stone;0" into id & meta
                        val (id, metaStr) = rawKey.split(";", limit = 2)
                        val block = Block.getBlockFromName(id) ?: return@forEach
                        val meta  = metaStr.toIntOrNull() ?: 0
                        block.getStateFromMeta(meta)
                    }
                    else -> return@forEach
                }

                // 2) pair each position with that state
                positions.forEach { pos ->
                    list += state to pos
                }
            }

            out[area] = list
        }

        return out
    }




    val gson: Gson = GsonBuilder()
        .registerTypeAdapter(IBlockState::class.java, FMEBlockDeserializer()).registerTypeAdapter(BlockPos::class.java,
            BlockPosAdapter()).disableHtmlEscaping()
        .create()
    val floorsConfigType = object : TypeToken<Map<String, Map<IBlockState, Set<BlockPos>>>>() {}.type

    var floorsConfig: Map<String, Map<IBlockState, Set<BlockPos>>>? = null
    var config: Map<String, Map<IBlockState, Set<BlockPos>>>? = null

    fun loadConfig() {
        try {

            val configFile = File(this.path, "extrasConfig.json");

            // --- load 'config' ---
            val configJson = FileInputStream(configFile)
                .bufferedReader(Charset.forName("UTF-8"))
                .use { it.readText() }

            if (configJson.isNotBlank()) {
                @Suppress("UNCHECKED_CAST")
                config = gson.fromJson(
                    configJson,
                    object : TypeToken<Map<String, Map<IBlockState, Set<BlockPos>>>>() {}.type
                ) as Map<String, Map<IBlockState, Set<BlockPos>>>

            }


        } catch (e: Exception) {
            logger.error("error loading config", e)
        }

        try {
            val floorsConfigFile = File(this.path, "floorsConfig.json");

            // --- load 'floorsConfig' ---
            val floorsJson = FileInputStream(floorsConfigFile)
                .bufferedReader(Charset.forName("UTF-8"))
                .use { it.readText() }

            if (floorsJson.isNotBlank()) {
                @Suppress("UNCHECKED_CAST")
                floorsConfig = gson.fromJson(
                    floorsJson,
                    object : TypeToken<Map<String, Map<IBlockState, Set<BlockPos>>>>() {}.type
                ) as Map<String, Map<IBlockState, Set<BlockPos>>>
            }
        } catch (e: Exception) {
            logger.error("error loading floorsConfig", e)
        }
        val extrasFlattened = (config    ?: emptyMap<String, Map<*, Set<BlockPos>>>())
            .toBlockConfig()
        val floorsFlattened = (floorsConfig ?: emptyMap<String, Map<*, Set<BlockPos>>>())
            .toBlockConfig()

// merge them however you like
        val finalBlockConfig = mutableMapOf<String, MutableList<Pair<IBlockState, BlockPos>>>()
        finalBlockConfig.putAll(extrasFlattened)
        floorsFlattened.forEach { (area, list) ->
            finalBlockConfig.merge(area, list) { old, new ->
                old.apply { addAll(new) }
            }
        }

// hand into your Brush module
        Brush.blockConfig = finalBlockConfig
    }

}





class BlockPosAdapter : JsonSerializer<BlockPos>, JsonDeserializer<BlockPos> {

    override fun serialize(
        src: BlockPos,
        typeOfSrc: Type,
        context: JsonSerializationContext
    ): JsonElement {
        // "x, y, z"
        return JsonPrimitive("${src.x}, ${src.y}, ${src.z}")
    }

    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): BlockPos {
        return when {
            json.isJsonObject -> {
                val obj = json.asJsonObject
                val x = obj.get("x")?.asInt ?: 0
                val y = obj.get("y")?.asInt ?: 0
                val z = obj.get("z")?.asInt ?: 0
                BlockPos(x, y, z)
            }
            json.isJsonPrimitive && json.asJsonPrimitive.isString -> {
                val parts = json.asString.split(", ")
                val x = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val y = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val z = parts.getOrNull(2)?.toIntOrNull() ?: 0
                BlockPos(x, y, z)
            }
            else -> BlockPos(0, 0, 0)
        }
    }
}



class FMEBlockDeserializer : JsonDeserializer<IBlockState> {

    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): IBlockState {
        val fallback: IBlockState = IBlockStateUtils.airIBlockState

        if (json == null || !json.isJsonPrimitive) {
            return fallback
        }

        val raw: String = json.asString
        val hasArgs = raw.contains("[") && raw.contains("]")

        // No properties: just a bare block name
        if (!hasArgs) {
            val block = Block.getBlockFromName(raw)
            return block?.defaultState ?: fallback
        }

        // Parse the part before “[”
        val baseName = raw.substringBefore("[")
        val block = Block.getBlockFromName(baseName)
        var state: IBlockState = block?.defaultState ?: fallback

        // Extract “key=value” pairs inside the brackets
        val inner = raw.substringAfter("[").substringBefore("]")
        val args = inner.split(",")

        for (arg in args) {
            val parts = arg.split("=", limit = 2)
            if (parts.size != 2) continue

            val key = parts[0]
            val value = parts[1]

            // Find the matching property on this block-state
            val prop = state.propertyNames
                .firstOrNull { it.name == key } as? IProperty<*>
                ?: continue

            // Apply the property, yielding a new IBlockState
            state = IBlockStateUtils.withProperty(state, prop, value)
        }

        return state
    }


}