package noobroutes.utils.skyblock.dungeon

import net.minecraft.util.BlockPos
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom

object DoorPositions {
    val oneByOneDoors = listOf(
        BlockPos(0, 69, 16),
        BlockPos(-16, 69, 0),
        BlockPos(0, 69, -16),
        BlockPos(16, 69, 0)
    )
    val twoByOneDoors = listOf(
        BlockPos(-32, 69, 0),
        BlockPos(-16, 69, -16),
        BlockPos(16, 69, -16),
        BlockPos(32, 69, 0),
        BlockPos(16, 69, 16),
        BlockPos(-16, 69, 16)
    )
    val threeByOneDoors = listOf(
        BlockPos(48, 69, 0),
        BlockPos(32, 69, 16),
        BlockPos(0, 69, 16),
        BlockPos(-32, 69, 16),
        BlockPos(-48, 69, 0),
        BlockPos(-32, 69, -16),
        BlockPos(0, 69, -16),
        BlockPos(32, 69, -16)
    )
    val fourByOneDoors = listOf(
        BlockPos(-64, 69, 0),
        BlockPos(-48, 69, -16),
        BlockPos(-16, 69, -16),
        BlockPos(16, 69, -16),
        BlockPos(48, 69, -16),
        BlockPos(64, 69, 0),
        BlockPos(48, 69, 16),
        BlockPos(16, 69, 16),
        BlockPos(-16, 69, 16),
        BlockPos(-48, 69, 16),
    )
    val lShapedDoors = listOf(
        BlockPos(32, 69, -16),
        BlockPos(32, 69, 16),
        BlockPos(16, 69, 32),
        BlockPos(0, 69, 16),
        BlockPos(-16, 69, 0),
        BlockPos(-32, 69, -16),
        BlockPos(-16, 69, -32),
        BlockPos(16, 69, -32)
    )
    val twoByTwoDoors = listOf(
        BlockPos(32, 69, -16),
        BlockPos(32, 69, 16),
        BlockPos(16, 69, 32),
        BlockPos(-16, 69, 32),
        BlockPos(-32, 69, 16),
        BlockPos(-32, 69, -16),
        BlockPos(-16, 69, -32),
        BlockPos(16, 69, -32),
    )
    val threeByOneSpots = mapOf(
        0 to Pair(BlockPos(46, 68, -1), BlockPos(46, 68, 1)),
        1 to Pair(BlockPos(33, 68, 14), BlockPos(31, 68, 14)),
        2 to Pair(BlockPos(1, 68, 14), BlockPos(-1, 68, 14)),
        3 to Pair(BlockPos(-31, 68, 14), BlockPos(-33, 68, 14)),
        4 to Pair(BlockPos(-46, 68, 1), BlockPos(-46, 68, -1)),
        5 to Pair(BlockPos(-33, 68, -14), BlockPos(-31, 68, -14)),
        6 to Pair(BlockPos(-1, 68, -14), BlockPos(1, 68, -14)),
        7 to Pair(BlockPos(31, 68, -14), BlockPos(33, 68, -14)),
    )
    val lShapedSpots = mapOf(
        0 to Pair(BlockPos(30, 68, -17), BlockPos(30, 68, -15)),
        1 to Pair(BlockPos(30, 68, 15), BlockPos(30, 68, 17)),
        2 to Pair(BlockPos(17, 68, 30), BlockPos(15, 68, 30)),
        3 to Pair(BlockPos(2, 68, 17), BlockPos(2, 68, 15)),
        4 to Pair(BlockPos(-15, 68, -2), BlockPos(-17, 68, -2)),
        5 to Pair(BlockPos(-30, 68, -15), BlockPos(-30, 68, -17)),
        6 to Pair(BlockPos(-17, 68, -30), BlockPos(-15, 68, -30)),
        7 to Pair(BlockPos(15, 68, -30), BlockPos(17, 68, -30))
    )
    val fourByOneSpots = mapOf(
        0 to Pair(BlockPos(-62, 68, 1), BlockPos(-62, 68, -1)),
        1 to Pair(BlockPos(-49, 68, -14), BlockPos(-47, 68, -14)),
        2 to Pair(BlockPos(-17, 68, -14), BlockPos(-15, 68, -14)),
        3 to Pair(BlockPos(15, 68, -14), BlockPos(17, 68, -14)),
        4 to Pair(BlockPos(47, 68, -14), BlockPos(49, 68, -14)),
        5 to Pair(BlockPos(62, 68, -1), BlockPos(62, 68, 1)),
        6 to Pair(BlockPos(49, 68, 14), BlockPos(47, 68, 14)),
        7 to Pair(BlockPos(17, 68, 14), BlockPos(15, 68, 14)),
        8 to Pair(BlockPos(-15, 68, 14), BlockPos(-17, 68, 14)),
        9 to Pair(BlockPos(-47, 68, 14), BlockPos(-49, 68, 14))
    )
    val oneByOneSpots = mapOf(
        0 to Pair(BlockPos(1, 68, 14), BlockPos(-1, 68, 14)),
        1 to Pair(BlockPos(-14, 68, 1), BlockPos(-14, 68, -1)),
        2 to Pair(BlockPos(-1, 68, -14), BlockPos(1, 68, -14)),
        3 to Pair(BlockPos(14, 68, -1), BlockPos(14, 68, 1))
    )
    val twoByOneSpots = mapOf(
        0 to Pair(BlockPos(-30, 68, 1), BlockPos(-30, 68, -1)),
        1 to Pair(BlockPos(-17, 68, -14), BlockPos(-15, 68, -14)),
        2 to Pair(BlockPos(15, 68, -14), BlockPos(17, 68, -14)),
        3 to Pair(BlockPos(30, 68, -1), BlockPos(30, 68, 1)),
        4 to Pair(BlockPos(17, 68, 14), BlockPos(15, 68, 14)),
        5 to Pair(BlockPos(-15, 68, 14), BlockPos(-17, 68, 14))
    )
    val twoByTwoSpots = mapOf(
        0 to Pair(BlockPos(30, 68, -17), BlockPos(30, 68, -15)),
        1 to Pair(BlockPos(30, 68, 15), BlockPos(30, 68, 17)),
        2 to Pair(BlockPos(17, 68, 30), BlockPos(15, 68, 30)),
        3 to Pair(BlockPos(-15, 68, 30), BlockPos(-17, 68, 30)),
        4 to Pair(BlockPos(-30, 68, 17), BlockPos(-30, 68, 15)),
        5 to Pair(BlockPos(-30, 68, -15), BlockPos(-30, 68, -17)),
        6 to Pair(BlockPos(-17, 68, -30), BlockPos(-15, 68, -30)),
        7 to Pair(BlockPos(15, 68, -30), BlockPos(17, 68, -30))
    )
    val blockList = mapOf(
        "minecraft:cobblestone_wall" to listOf(
            BlockPos(2, 0, 2),
            BlockPos(2, 2, 2),
            BlockPos(2, 0, -2),
            BlockPos(2, 2, -2),
            BlockPos(-2, 0, 2),
            BlockPos(-2, 2, 2),
            BlockPos(-2, 0, -2),
            BlockPos(-2, 2, -2)
        ),
        "minecraft:iron_bars" to listOf(
            BlockPos(2, 1, 2),
            BlockPos(2, 1, -2),
            BlockPos(-2, 1, 2),
            BlockPos(-2, 1, -2)
        ),
        "minecraft:stone_brick_stairs" to listOf(
            BlockPos(2, 3, 2),
            BlockPos(2, 3, 1),
            BlockPos(2, 3, -1),
            BlockPos(2, 3, -2),
            BlockPos(-2, 3, 2),
            BlockPos(-2, 3, 1),
            BlockPos(-2, 3, -1),
            BlockPos(-2, 3, -2),
        ),
        "minecraft:stonebrick" to listOf(
            BlockPos(2, 4, 2),
            BlockPos(2, 4, 1),
            BlockPos(2, 4, 0),
            BlockPos(2, 4, -1),
            BlockPos(2, 4, -2),
            BlockPos(1, 0, 2),
            BlockPos(1, 1, 2),
            BlockPos(1, 2, 2),
            BlockPos(1, 3, 2),
            BlockPos(1, 4, 2),
            BlockPos(1, 4, 1),
            BlockPos(1, 4, 0),
            BlockPos(1, 4, -1),
            BlockPos(1, 0, -2),
            BlockPos(1, 1, -2),
            BlockPos(1, 2, -2),
            BlockPos(1, 3, -2),
            BlockPos(1, 4, -2),
            BlockPos(0, 0, 2),
            BlockPos(0, 1, 2),
            BlockPos(0, 2, 2),
            BlockPos(0, 3, 2),
            BlockPos(0, 4, 2),
            BlockPos(0, 4, 1),
            BlockPos(0, 4, 0),
            BlockPos(0, 4, -1),
            BlockPos(0, 0, -2),
            BlockPos(0, 1, -2),
            BlockPos(0, 2, -2),
            BlockPos(0, 3, -2),
            BlockPos(0, 4, -2),
            BlockPos(-1, 0, 2),
            BlockPos(-1, 1, 2),
            BlockPos(-1, 2, 2),
            BlockPos(-1, 3, 2),
            BlockPos(-1, 4, 2),
            BlockPos(-1, 4, 1),
            BlockPos(-1, 4, 0),
            BlockPos(-1, 4, -1),
            BlockPos(-1, 0, -2),
            BlockPos(-1, 1, -2),
            BlockPos(-1, 2, -2),
            BlockPos(-1, 3, -2),
            BlockPos(-1, 4, -2),
            BlockPos(-2, 4, 2),
            BlockPos(-2, 4, 1),
            BlockPos(-2, 4, 0),
            BlockPos(-2, 4, -1),
            BlockPos(-2, 4, -2),
        ),
        "minecraft:stone_slab" to listOf(
            BlockPos(2, 3, 0),
            BlockPos(-2, 3, 0)
        )
    )
    val room1x2Names = hashSetOf(
        "Gold",
        "Skull",
        "Archway",
        "Grass Ruin",
        "Redstone Warrior",
        "Balcony",
        "Grand Library",
        "Mage",
        "Crypt",
        "Doors",
        "Pedestal",
        "Purple Flags",
        "Bridges",
        "Pressure Plates"
    )
    val room1x3Names = hashSetOf(
        "Diagonal",
        "Red Blue",
        "Wizard",
        "Slime",
        "Catwalk",
        "Deathmite",
        "Gravel"
    )
    val room1x4Names = hashSetOf(
        "Hallway",
        "Mossy",
        "Pit",
        "Pipes",
        "Quartz Knight",
        "Waterfall"
    )
    val room2x2Names = hashSetOf(
        "Stairs",
        "Buttons",
        "Museum",
        "Atlas",
        "Supertall",
        "Flags",
        "Cathedral",
        "Rails",
        "Mines"
    )
    val roomLShapedNames = hashSetOf(
        "Dino Site",
        "Withermancer",
        "Chambers",
        "Market",
        "Lava Ravine",
        "Melon",
        "Well",
        "Layers",
        "Spider",
        "Pirate",
        "Altar"
    )


    fun getRoomDoors(room: UniqueRoom): List<BlockPos> {
        return when {
            room1x2Names.contains(room.name) -> twoByOneDoors
            room1x3Names.contains(room.name) -> threeByOneDoors
            roomLShapedNames.contains(room.name) -> lShapedDoors
            room1x4Names.contains(room.name) -> fourByOneDoors
            room2x2Names.contains(room.name) -> twoByTwoDoors
            else -> oneByOneDoors
        }
    }

    fun getDoorSpots(room: UniqueRoom): Map<Int, Pair<BlockPos, BlockPos>> {
        return when {
            room1x2Names.contains(room.name) -> twoByOneSpots
            room1x3Names.contains(room.name) -> threeByOneSpots
            roomLShapedNames.contains(room.name) -> lShapedSpots
            room1x4Names.contains(room.name) -> fourByOneSpots
            room2x2Names.contains(room.name) -> twoByTwoSpots
            else -> oneByOneSpots
        }
    }


    private const val WITHER_SKULL_ID = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2JjYmJmOTRkNjAzNzQzYTFlNzE0NzAyNmUxYzEyNDBiZDk4ZmU4N2NjNGVmMDRkY2FiNTFhMzFjMzA5MTRmZCJ9fX0="
    private const val BLOOD_SKULL_ID = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWQ5ZDgwYjc5NDQyY2YxYTNhZmVhYTIzN2JkNmFkYWFhY2FiMGMyODgzMGZiMzZiNTcwNGNmNGQ5ZjU5MzdjNCJ9fX0="

}