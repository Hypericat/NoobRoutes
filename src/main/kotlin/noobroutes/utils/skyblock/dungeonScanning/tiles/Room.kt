package noobroutes.utils.skyblock.dungeonScanning.tiles

import noobroutes.utils.Vec2i
import noobroutes.utils.skyblock.dungeonScanning.RoomData


class Room(override val x: Int, override val z: Int, var data: RoomData) : Tile {
    var core = 0
    var isSeparator = false
    var uniqueRoom: UniqueRoom? = null
    override var state: RoomState = RoomState.UNDISCOVERED
    val vec2i = Vec2i(x, z)
}