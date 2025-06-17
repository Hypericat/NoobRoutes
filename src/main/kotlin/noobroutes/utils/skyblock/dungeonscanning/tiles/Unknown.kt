package noobroutes.utils.skyblock.dungeonscanning.tiles


class Unknown(override val x: Int, override val z: Int) : Tile {
    override var state: RoomState = RoomState.UNDISCOVERED
}