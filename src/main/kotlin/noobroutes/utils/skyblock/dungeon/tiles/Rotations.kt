package noobroutes.utils.skyblock.dungeon.tiles


import noobroutes.utils.Vec2i

enum class Rotations(
    val normal: Vec2i,
    val oneByFour: Vec2i
) {
    NORTH(Vec2i(15, 15), Vec2i(0, 7)),
    SOUTH(Vec2i( -15, -15), Vec2i(0, -7)),
    WEST(Vec2i(15, -15), Vec2i(7, 0)),
    EAST(Vec2i(-15, 15), Vec2i(-7, 0)),
    NONE(Vec2i(0, 0), Vec2i(0, 0));
}