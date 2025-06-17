package noobroutes.utils.skyblock.dungeonscanning

import noobroutes.utils.skyblock.dungeonscanning.tiles.RoomType

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
