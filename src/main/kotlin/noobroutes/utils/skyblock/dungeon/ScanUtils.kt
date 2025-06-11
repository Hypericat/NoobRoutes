package noobroutes.utils.skyblock.dungeon

import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.Core.logger
import noobroutes.Core.mc
import noobroutes.events.BossEventDispatcher.inBoss
import noobroutes.events.impl.RoomEnterEvent
import noobroutes.utils.Vec2i
import noobroutes.utils.equalsOneOf
import noobroutes.utils.getBlockIdAt
import noobroutes.utils.postAndCatch
import noobroutes.utils.skyblock.Island
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.tiles.*
import java.io.FileNotFoundException

object ScanUtils {
    private const val ROOM_SIZE_SHIFT = 5  // Since ROOM_SIZE = 32 (2^5) so we can perform bitwise operations
    private const val START = -185

    private var lastRoomPos: Vec2i =
        Vec2i(0, 0)
    private val roomList: Set<RoomData> = loadRoomData()
    var currentRoom: Room? = null
        private set
    var passedRooms: MutableSet<Room> = mutableSetOf()
        private set

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

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END || mc.theWorld == null || mc.thePlayer == null) return
        if ((!DungeonUtils.inDungeons) || inBoss
        ) {
            currentRoom?.let { RoomEnterEvent(null)
                .postAndCatch() }
            return
        } // We want the current room to register as null if we are not in a dungeon

        val roomCenter = getRoomCenter(PlayerUtils.posX.toInt(), PlayerUtils.posZ.toInt())

        if (!mc.theWorld.getChunkFromChunkCoords(roomCenter.x shr 4, roomCenter.z shr 4).isLoaded) return
        if (roomCenter == lastRoomPos && LocationUtils.currentArea.isArea(
                Island.SinglePlayer)) return // extra SinglePlayer caching for invalid placed rooms
        lastRoomPos = roomCenter
        passedRooms.find { previousRoom -> previousRoom.roomComponents.any { it.vec2i == roomCenter } }?.let { room ->
            if (currentRoom?.roomComponents?.none { it.vec2i == roomCenter } == true) RoomEnterEvent(
                room
            ).postAndCatch()
            return
        } // We want to use cached rooms instead of scanning it again if we have already passed through it and if we are already in it we don't want to trigger the event

        scanRoom(roomCenter)?.let { room -> if (room.rotation != Rotations.NONE) RoomEnterEvent(
            room
        ).postAndCatch() }
    }

    private fun updateRotation(room: Room) {
        val roomHeight = getTopLayerOfRoom(room.roomComponents.first().vec2i)
        if (room.data.name == "Fairy") { // Fairy room doesn't have a clay block so we need to set it manually
            room.clayPos = room.roomComponents.firstOrNull()?.let { BlockPos(it.x - 15, roomHeight, it.z - 15) } ?: return
            room.rotation = Rotations.SOUTH
            return
        }
        room.rotation = Rotations.entries.dropLast(1).find { rotation ->
            room.roomComponents.any { component ->
                BlockPos(component.x + rotation.x, roomHeight, component.z + rotation.z).let { blockPos ->
                    getBlockIdAt(blockPos) == 159 && (room.roomComponents.size == 1 || EnumFacing.HORIZONTALS.all { facing ->
                        getBlockIdAt(
                            blockPos.add(
                                facing.frontOffsetX,
                                0,
                                facing.frontOffsetZ
                            )
                        ).equalsOneOf(159, 0)
                    }).also { isCorrectClay -> if (isCorrectClay) room.clayPos = blockPos }
                }
            }
        } ?: Rotations.NONE // Rotation isn't found if we can't find the clay block
    }

    fun scanRoom(vec2i: Vec2i): Room? =
        getCore(vec2i).let { core -> getRoomData(core)?.let {
            Room(
                data = it,
                roomComponents = findRoomComponentsRecursively(vec2i, it.cores)
            )
        }?.apply { updateRotation(this) } }

    private fun findRoomComponentsRecursively(vec2i: Vec2i, cores: List<Int>, visited: MutableSet<Vec2i> = mutableSetOf(), tiles: MutableSet<RoomComponent> = mutableSetOf()): MutableSet<RoomComponent> {
        if (vec2i in visited) return tiles else visited.add(vec2i)
        tiles.add(
            RoomComponent(
                vec2i.x,
                vec2i.z,
                getCore(vec2i).takeIf { it in cores } ?: return tiles))

        EnumFacing.HORIZONTALS.forEach { facing ->
            findRoomComponentsRecursively(
                Vec2i(
                    vec2i.x + (facing.frontOffsetX shl ROOM_SIZE_SHIFT),
                    vec2i.z + (facing.frontOffsetZ shl ROOM_SIZE_SHIFT)
                ), cores, visited, tiles)
        }
        return tiles
    }

    private fun getRoomData(hash: Int): RoomData? =
        roomList.find { hash in it.cores }

    fun getRoomCenter(posX: Int, posZ: Int): Vec2i {
        val roomX = (posX - START + (1 shl (ROOM_SIZE_SHIFT - 1))) shr ROOM_SIZE_SHIFT
        val roomZ = (posZ - START + (1 shl (ROOM_SIZE_SHIFT - 1))) shr ROOM_SIZE_SHIFT
        return Vec2i(
            ((roomX shl ROOM_SIZE_SHIFT) + START),
            ((roomZ shl ROOM_SIZE_SHIFT) + START)
        )
    }

    fun getCore(vec2i: Vec2i): Int {
        val sb = StringBuilder(150)
        val chunk = mc.theWorld?.getChunkFromChunkCoords(vec2i.x shr 4, vec2i.z shr 4) ?: return 0
        val height = chunk.getHeightValue(vec2i.x and 15, vec2i.z and 15).coerceIn(11..140)
        sb.append(CharArray(140 - height) { '0' })
        var bedrock = 0
        for (y in height downTo 12) {
            val id = Block.getIdFromBlock(chunk.getBlock(BlockPos(vec2i.x, y, vec2i.z)))
            if (id == 0 && bedrock >= 2 && y < 69) {
                sb.append(CharArray(y - 11) { '0' })
                break
            }

            if (id == 7) bedrock++
            else {
                bedrock = 0
                if (id.equalsOneOf(5, 54, 146)) continue
            }
            sb.append(id)
        }
        return sb.toString().hashCode()
    }

    private fun getTopLayerOfRoom(vec2i: Vec2i): Int {
        val chunk = mc.theWorld?.getChunkFromChunkCoords(vec2i.x shr 4, vec2i.z shr 4) ?: return 0
        val height = chunk.getHeightValue(vec2i.x and 15, vec2i.z and 15) - 1
        return if (chunk.getBlock(vec2i.x, height, vec2i.z) == Blocks.gold_block) height - 1 else height
    }

    fun addCachedRoom(room: Room?){
        if (passedRooms.none { it.data.name == room?.data?.name }) {
            passedRooms.add(room ?: return)
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun enterDungeonRoom(event: RoomEnterEvent) {
        currentRoom = event.room
        addCachedRoom(currentRoom)
        devMessage("${event.room?.data?.name} - ${event.room?.rotation} || clay: ${event.room?.clayPos}")
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Unload) {
        passedRooms.clear()
        currentRoom = null
        lastRoomPos = Vec2i(0, 0)
    }
}