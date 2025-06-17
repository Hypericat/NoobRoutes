package noobroutes.utils.skyblock.dungeonscanning.tiles

import noobroutes.utils.Vec2i


interface Tile {
    val x: Int
    val z: Int
    var state: RoomState
}