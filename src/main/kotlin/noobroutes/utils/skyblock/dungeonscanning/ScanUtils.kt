package noobroutes.utils.skyblock.dungeonscanning

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import net.minecraft.block.Block
import net.minecraft.util.BlockPos
import net.minecraft.util.ResourceLocation
import noobroutes.Core.logger
import noobroutes.Core.mc
import noobroutes.utils.equalsOneOf
import noobroutes.utils.skyblock.dungeonscanning.tiles.Room
import noobroutes.utils.skyblock.dungeonscanning.tiles.RoomType
import java.io.FileNotFoundException
import java.lang.reflect.Type
import kotlin.collections.orEmpty
import kotlin.div
import kotlin.math.roundToInt

object ScanUtils {

    private val roomList: Set<RoomData> = loadRoomData()

    fun getRoomData(x: Int, z: Int): RoomData? {
        return getRoomData(getCore(x, z))
    }

    fun getRoomData(hash: Int): RoomData? {
        return roomList.find { hash in it.cores }
    }

    fun getRoomCentre(posX: Int, posZ: Int): Pair<Int, Int> {
        val roomX = ((posX - DungeonScan.startX) / 32f).roundToInt()
        val roomZ = ((posZ - DungeonScan.startZ) / 32f).roundToInt()
        return Pair(roomX * 32 + DungeonScan.startX, roomZ * 32 + DungeonScan.startZ)
    }

    fun getRoomFromPos(pos: BlockPos): Room? {
        val x = ((pos.x - DungeonScan.startX + 15) shr 5)
        val z = ((pos.z - DungeonScan.startZ + 15) shr 5)
        val room = Dungeon.Info.dungeonList.getOrNull(x * 2 + z * 22)
        return room as? Room
    }


    class RoomDataDeserializer : JsonDeserializer<RoomData> {
        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): RoomData {
            val jsonObject = json?.asJsonObject
            val name = jsonObject?.get("name")?.asString ?: ""
            val type = context?.deserialize(jsonObject?.get("type"), RoomType::class.java) ?: RoomType.NORMAL
            val coresType = object : TypeToken<List<Int>>() {}.type
            val cores = context?.deserialize<List<Int>>(jsonObject?.get("cores"), coresType).orEmpty()
            val crypts = jsonObject?.get("crypts")?.asInt ?: 0
            val secrets = jsonObject?.get("secrets")?.asInt ?: 0

            return RoomData(name, type, cores, crypts, secrets)
        }
    }

    private fun loadRoomData(): Set<RoomData> {
        return try {
            GsonBuilder()
                .registerTypeAdapter(
                    RoomData::class.java,
                    RoomDataDeserializer()
                )
                .create().fromJson(
                    (ScanUtils::class.java.getResourceAsStream("/rooms.json") ?: throw FileNotFoundException()).bufferedReader(),
                    object : TypeToken<Set<RoomData>>() {}.type
                )
        } catch (e: Exception) {
            handleRoomDataError(e)
            setOf()
        }
    }

    private fun handleRoomDataError(e: Exception) {
        when (e) {
            is JsonSyntaxException -> println("Error parsing room data.")
            is JsonIOException -> println("Error reading room data.")
            is FileNotFoundException -> println("Room data not found, something went wrong! Please report this!")
            else -> {
                println("Unknown error while reading room data.")
                logger.error("Error reading room data", e)
                println(e.message)
            }
        }
    }


    fun getCore(x: Int, z: Int): Int {
        val sb = StringBuilder(150)
        val chunk = mc.theWorld.getChunkFromChunkCoords(x shr 4, z shr 4)
        val height = chunk.getHeightValue(x and 15, z and 15).coerceIn(11..140)
        sb.append(CharArray(140 - height) { '0' })
        var bedrock = 0
        for (y in height downTo 12) {
            val id = Block.getIdFromBlock(chunk.getBlock(BlockPos(x, y, z)))
            if (id == 0 && bedrock >= 2 && y < 69) {
                sb.append(CharArray(y - 11) { '0' })
                break
            }

            if (id == 7) {
                bedrock++
            } else {
                bedrock = 0
                if (id.equalsOneOf(5, 54, 146)) continue
            }

            sb.append(id)
        }
        return sb.toString().hashCode()
    }
}