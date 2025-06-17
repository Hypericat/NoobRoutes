package noobroutes.utils.skyblock.dungeonScanning.tiles


interface Tile {
    val x: Int
    val z: Int
    var state: RoomState
}