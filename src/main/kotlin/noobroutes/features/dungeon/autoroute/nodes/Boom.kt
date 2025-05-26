package noobroutes.features.dungeon.autoroute.nodes

import com.google.gson.JsonObject
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.dungeon.autoroute.Node
import noobroutes.utils.skyblock.dungeon.tiles.Room

class Boom(
    pos: Vec3 = Vec3(0.0, 0.0, 0.0),
    var target: BlockPos = BlockPos(0.0, 0.0, 0.0),
    awaitSecret: Int = 0,
    maybeSecret: Boolean = false,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
) : Node(
    "Boom",
    1,
    pos,
    awaitSecret,
    maybeSecret,
    delay,
    center,
    stop,
    chain
) {

    override fun tick(room: Room) {
        super.tick(room)
    }
    override fun render(room: Room) {
        drawNode(room, AutoRoute.boomColor)
    }

    override fun nodeAddInfo(obj: JsonObject) {
        TODO("Not yet implemented")
    }

    override fun loadNodeInfo(obj: JsonObject) {
        TODO("Not yet implemented")
    }

}