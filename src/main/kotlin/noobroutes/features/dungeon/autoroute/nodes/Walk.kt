package noobroutes.features.dungeon.autoroute.nodes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autoroute.Node
import noobroutes.features.floor7.autop3.AutoP3.depth
import noobroutes.utils.AutoP3Utils.startWalk
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealYaw
import noobroutes.utils.skyblock.dungeon.tiles.Room

class Walk(
    pos: Vec3,
    var yaw: Float,
    awaitSecret: Int = 0,
    maybeSecret: Boolean = false,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
) : Node("Walk", pos = pos, awaitSecrets = awaitSecret, maybeSecret = maybeSecret, delay = delay, center = center, stop = stop, chain = chain) {
    override fun run(
        event: MotionUpdateEvent.Pre,
        room: Room
    ) {
        startWalk(room.getRealYaw(yaw))
    }

    override fun render(room: Room) {
        Renderer.drawCylinder(room.getRealCoords(pos.add(Vec3(0.0, 0.03, 0.0))), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.GREEN, depth = depth)
    }

    override fun getAsJsonObject(): JsonObject {
        TODO("Not yet implemented")
    }


}