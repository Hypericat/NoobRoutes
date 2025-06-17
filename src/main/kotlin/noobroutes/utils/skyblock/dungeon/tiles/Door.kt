package noobroutes.utils.skyblock.dungeon.tiles


class Door(override val x: Int, override val z: Int, var type: DoorType) : Tile {
    var opened = false
    override var state: RoomState = RoomState.UNDISCOVERED
}