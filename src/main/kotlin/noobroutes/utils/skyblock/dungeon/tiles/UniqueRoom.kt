package noobroutes.utils.skyblock.dungeon.tiles

import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import noobroutes.Core.mc
import noobroutes.utils.Vec2i
import noobroutes.utils.equalsOneOf
import noobroutes.utils.getBlockIdAt
import noobroutes.utils.skyblock.dungeon.Dungeon


class UniqueRoom(arrX: Int, arrY: Int, room: Room) {
    var name: String
    var topLeft = Vec2i(arrX, arrY)
    var center = Vec2i(arrX, arrY)
        private set
    var mainRoom = room
    val tiles = mutableListOf(room to Vec2i(arrX, arrY))
    var rotation: Rotations = Rotations.NONE
    var clayPos: BlockPos = BlockPos(0, 0, 0)

    val roomComponents get() = tiles.filter { !it.first.isSeparator }


    init {
        if (room.data.name == "Unknown") {
            name = "Unknown_${arrX}_${arrY}"
        } else {
            name = room.data.name
            init(arrX, arrY, room)
        }
        room.uniqueRoom = this
        Dungeon.Info.uniqueRooms.add(this)
    }




    fun init(arrX: Int, arrY: Int, room: Room) {
        when (room.data.type) {
            RoomType.TRAP -> Dungeon.Info.trapType = room.data.name.split(" ")[0]
            RoomType.PUZZLE -> Puzzle.fromName(room.data.name)?.let { Dungeon.Info.puzzles.putIfAbsent(it, false) }
            else -> {}
        }
    }
    fun addTile(x: Int, y: Int, tile: Room) {
        addToTiles(x, y, tile)
        calculateCenter()
    }

    fun addTiles(tiles: Iterable<Pair<Int, Int>>) {
        tiles.forEach { (x, y) ->
            val room = Dungeon.Info.dungeonList[y * 11 + x] as? Room ?: return@forEach
            if (room.uniqueRoom !== this) {
                Dungeon.Info.uniqueRooms.remove(room.uniqueRoom)
                addToTiles(x, y, room)
            }
        }
        calculateCenter()
    }
    private fun addToTiles(x: Int, y: Int, tile: Room) {
        if (mainRoom.data.name == "Unknown") {
            if (tile.data.name != "Unknown") {
                init(x, y, tile)
                name = tile.data.name
                mainRoom.data = tile.data
            }
        } else if (tile.data.name == "Unknown") {
            tile.data = mainRoom.data
        }

        tile.uniqueRoom = this

        tiles.removeIf { it.first.x == tile.x && it.first.z == tile.z }
        tiles.add(tile to Vec2i(x, y))

        if (x < topLeft.x || (x == topLeft.x && y < topLeft.z)) {
            topLeft = Vec2i(x, y)
            mainRoom = tile
            if (name.startsWith("Unknown")) {
                name = "Unknown_${x}_${y}"
            }
        }
    }

    private fun calculateCenter() {
        if (tiles.size == 1) {
            center = tiles.first().second
            return
        }

        val positions = tiles.mapNotNull {
            it.second.takeIf { (arrX, arrZ) ->
                arrX % 2 == 0 && arrZ % 2 == 0
            }
        }

        if (positions.isEmpty()) return

        val xRooms = positions.groupBy { it.x }.entries.sortedByDescending { it.value.size }
        val zRooms = positions.groupBy { it.z }.entries.sortedByDescending { it.value.size }

        center = when {
            zRooms.size == 1 || zRooms[0].value.size != zRooms[1].value.size -> {
                Vec2i((xRooms.sumOf { it.key } / xRooms.size), zRooms[0].key)
            }

            xRooms.size == 1 || xRooms[0].value.size != xRooms[1].value.size -> {
                Vec2i(xRooms[0].key, zRooms.sumOf { it.key } / zRooms.size)
            }

            else -> (Vec2i((xRooms[0].key + xRooms[1].key) / 2, (zRooms[0].key + zRooms[1].key) / 2))
        }
    }

    fun getTopLayerOfRoom(vec2i: Vec2i): Int {
        val chunk = mc.theWorld?.getChunkFromChunkCoords(vec2i.x shr 4, vec2i.z shr 4) ?: return 0
        val height = chunk.getHeightValue(vec2i.x and 15, vec2i.z and 15) - 1
        return if (chunk.getBlock(vec2i.x, height, vec2i.z) == Blocks.gold_block) height - 1 else height
    }

    fun updateRotation() {
        val roomHeight = getTopLayerOfRoom(roomComponents.firstOrNull()?.first?.vec2i ?: return)
        if (name == "Fairy") { // Fairy room doesn't have a clay block so we need to set it manually
            clayPos = roomComponents.firstOrNull()?.let { BlockPos(it.first.x - 15, roomHeight, it.first.z - 15) } ?: return
            rotation = Rotations.SOUTH
            return
        }

        rotation = Rotations.entries.dropLast(1).find { rotation ->
            roomComponents.any { component ->
                BlockPos(component.first.x + rotation.x, roomHeight, component.first.z + rotation.z).let { blockPos ->
                    getBlockIdAt(blockPos) == 159 && (roomComponents.size == 1 || EnumFacing.HORIZONTALS.all { facing ->
                        getBlockIdAt(
                            blockPos.add(
                                facing.frontOffsetX,
                                0,
                                facing.frontOffsetZ
                            )
                        ).equalsOneOf(159, 0)
                    }).also { isCorrectClay -> if (isCorrectClay) clayPos = blockPos }
                }
            }
        } ?: return // Rotation isn't found if we can't find the clay block
        //devMessage(rotation)
    }





}