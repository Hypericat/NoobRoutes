package com.github.wadey3636.noobroutes.utils.skyblock.dungeon

import com.github.wadey3636.noobroutes.Core.logger
import com.github.wadey3636.noobroutes.Core.mc
import com.github.wadey3636.noobroutes.utils.equalsOneOf
import com.github.wadey3636.noobroutes.utils.postAndCatch
import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.*
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import java.io.FileNotFoundException
import com.github.wadey3636.noobroutes.utils.getBlockIdAt

object ScanUtils {
    private const val ROOM_SIZE_SHIFT = 5  // Since ROOM_SIZE = 32 (2^5) so we can perform bitwise operations
    private const val START = -185

    private var lastRoomPos: com.github.wadey3636.noobroutes.utils.Vec2 =
        _root_ide_package_.com.github.wadey3636.noobroutes.utils.Vec2(0, 0)
    private val roomList: Set<com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.RoomData> = loadRoomData()
    var currentRoom: com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.Room? = null
        private set
    var passedRooms: MutableSet<com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.Room> = mutableSetOf()
        private set

    private fun loadRoomData(): Set<com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.RoomData> {
        return try {
            GsonBuilder()
                .registerTypeAdapter(
                    _root_ide_package_.com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.RoomData::class.java,
                    _root_ide_package_.com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.RoomDataDeserializer()
                )
                .create().fromJson(
                    (ScanUtils::class.java.getResourceAsStream("/rooms.json") ?: throw FileNotFoundException()).bufferedReader(),
                    object : com.google.gson.reflect.TypeToken<Set<com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.RoomData>>() {}.type
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
        if ((!_root_ide_package_.com.github.wadey3636.noobroutes.utils.skyblock.dungeon.DungeonUtils.inDungeons && !_root_ide_package_.com.github.wadey3636.noobroutes.utils.skyblock.LocationUtils.currentArea.isArea(
                _root_ide_package_.com.github.wadey3636.noobroutes.utils.skyblock.Island.SinglePlayer)) || _root_ide_package_.com.github.wadey3636.noobroutes.utils.skyblock.dungeon.DungeonUtils.inBoss
        ) {
            currentRoom?.let { _root_ide_package_.com.github.wadey3636.noobroutes.events.impl.RoomEnterEvent(null)
                .postAndCatch() }
            return
        } // We want the current room to register as null if we are not in a dungeon

        val roomCenter = getRoomCenter(_root_ide_package_.com.github.wadey3636.noobroutes.utils.skyblock.PlayerUtils.posX.toInt(), _root_ide_package_.com.github.wadey3636.noobroutes.utils.skyblock.PlayerUtils.posZ.toInt())
        if (roomCenter == lastRoomPos && _root_ide_package_.com.github.wadey3636.noobroutes.utils.skyblock.LocationUtils.currentArea.isArea(
                _root_ide_package_.com.github.wadey3636.noobroutes.utils.skyblock.Island.SinglePlayer)) return // extra SinglePlayer caching for invalid placed rooms
        lastRoomPos = roomCenter

        passedRooms.find { previousRoom -> previousRoom.roomComponents.any { it.vec2 == roomCenter } }?.let { room ->
            if (currentRoom?.roomComponents?.none { it.vec2 == roomCenter } == true) _root_ide_package_.com.github.wadey3636.noobroutes.events.impl.RoomEnterEvent(
                room
            ).postAndCatch()
            return
        } // We want to use cached rooms instead of scanning it again if we have already passed through it and if we are already in it we don't want to trigger the event

        scanRoom(roomCenter)?.let { room -> if (room.rotation != _root_ide_package_.com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.Rotations.NONE) _root_ide_package_.com.github.wadey3636.noobroutes.events.impl.RoomEnterEvent(
            room
        ).postAndCatch() }
    }

    private fun updateRotation(room: com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.Room) {
        val roomHeight = getTopLayerOfRoom(room.roomComponents.first().vec2)
        if (room.data.name == "Fairy") { // Fairy room doesn't have a clay block so we need to set it manually
            room.clayPos = room.roomComponents.firstOrNull()?.let { BlockPos(it.x - 15, roomHeight, it.z - 15) } ?: return
            room.rotation = _root_ide_package_.com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.Rotations.SOUTH
            return
        }
        room.rotation = _root_ide_package_.com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.Rotations.entries.dropLast(1).find { rotation ->
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
        } ?: _root_ide_package_.com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.Rotations.NONE // Rotation isn't found if we can't find the clay block
    }

    private fun scanRoom(vec2: com.github.wadey3636.noobroutes.utils.Vec2): com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.Room? =
        getCore(vec2).let { core -> getRoomData(core)?.let {
            _root_ide_package_.com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.Room(
                data = it,
                roomComponents = findRoomComponentsRecursively(vec2, it.cores)
            )
        }?.apply { updateRotation(this) } }

    private fun findRoomComponentsRecursively(vec2: com.github.wadey3636.noobroutes.utils.Vec2, cores: List<Int>, visited: MutableSet<com.github.wadey3636.noobroutes.utils.Vec2> = mutableSetOf(), tiles: MutableSet<com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.RoomComponent> = mutableSetOf()): MutableSet<com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.RoomComponent> {
        if (vec2 in visited) return tiles else visited.add(vec2)
        tiles.add(
            _root_ide_package_.com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.RoomComponent(
                vec2.x,
                vec2.z,
                getCore(vec2).takeIf { it in cores } ?: return tiles))
        EnumFacing.HORIZONTALS.forEach { facing ->
            findRoomComponentsRecursively(
                _root_ide_package_.com.github.wadey3636.noobroutes.utils.Vec2(
                    vec2.x + (facing.frontOffsetX shl ROOM_SIZE_SHIFT),
                    vec2.z + (facing.frontOffsetZ shl ROOM_SIZE_SHIFT)
                ), cores, visited, tiles)
        }
        return tiles
    }

    private fun getRoomData(hash: Int): com.github.wadey3636.noobroutes.utils.skyblock.dungeon.tiles.RoomData? =
        roomList.find { hash in it.cores }

    fun getRoomCenter(posX: Int, posZ: Int): com.github.wadey3636.noobroutes.utils.Vec2 {
        val roomX = (posX - START + (1 shl (ROOM_SIZE_SHIFT - 1))) shr ROOM_SIZE_SHIFT
        val roomZ = (posZ - START + (1 shl (ROOM_SIZE_SHIFT - 1))) shr ROOM_SIZE_SHIFT
        return _root_ide_package_.com.github.wadey3636.noobroutes.utils.Vec2(
            (roomX shl ROOM_SIZE_SHIFT) + START,
            (roomZ shl ROOM_SIZE_SHIFT) + START
        )
    }

    fun getCore(vec2: com.github.wadey3636.noobroutes.utils.Vec2): Int {
        val sb = StringBuilder(150)
        val chunk = mc.theWorld?.getChunkFromChunkCoords(vec2.x shr 4, vec2.z shr 4) ?: return 0
        val height = chunk.getHeightValue(vec2.x and 15, vec2.z and 15).coerceIn(11..140)
        sb.append(CharArray(140 - height) { '0' })
        var bedrock = 0
        for (y in height downTo 12) {
            val id = Block.getIdFromBlock(chunk.getBlock(BlockPos(vec2.x, y, vec2.z)))
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

    private fun getTopLayerOfRoom(vec2: com.github.wadey3636.noobroutes.utils.Vec2): Int {
        val chunk = mc.theWorld?.getChunkFromChunkCoords(vec2.x shr 4, vec2.z shr 4) ?: return 0
        val height = chunk.getHeightValue(vec2.x and 15, vec2.z and 15) - 1
        return if (chunk.getBlock(vec2.x, height, vec2.z) == Blocks.gold_block) height - 1 else height
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun enterDungeonRoom(event: com.github.wadey3636.noobroutes.events.impl.RoomEnterEvent) {
        currentRoom = event.room
        if (passedRooms.none { it.data.name == currentRoom?.data?.name }) passedRooms.add(currentRoom ?: return)
        _root_ide_package_.com.github.wadey3636.noobroutes.utils.skyblock.devMessage("${event.room?.data?.name} - ${event.room?.rotation} || clay: ${event.room?.clayPos}")
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Unload) {
        passedRooms.clear()
        currentRoom = null
        lastRoomPos = _root_ide_package_.com.github.wadey3636.noobroutes.utils.Vec2(0, 0)
    }
}