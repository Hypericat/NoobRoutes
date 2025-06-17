package noobroutes.utils.skyblock.dungeon

import noobroutes.utils.skyblock.dungeon.tiles.RoomType

data class RoomData(
    val name: String,
    var type: RoomType,
    val cores: List<Int>,
    val crypts: Int,
    val secrets: Int
) {
    companion object {
        fun createUnknown(type: RoomType) = RoomData("Unknown", type, emptyList(), 0, 0)
    }
}
