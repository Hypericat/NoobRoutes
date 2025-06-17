package noobroutes.features.dungeon.autoroute.nodes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.dungeon.autoroute.AutoRoute.depth
import noobroutes.features.dungeon.autoroute.AutoRoute.silent
import noobroutes.features.dungeon.autoroute.AutoRoute.walkColor
import noobroutes.features.dungeon.autoroute.AutoRouteUtils.serverSneak
import noobroutes.features.dungeon.autoroute.Node
import noobroutes.utils.AutoP3Utils.startWalk
import noobroutes.utils.Scheduler
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealYaw
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom

class Walk(
    pos: Vec3 = Vec3(0.0, 0.0, 0.0),
    var yaw: Float = 0f,
    awaitSecret: Int = 0,
    maybeSecret: Boolean = false,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
    reset: Boolean = false,
) : Node("Walk", 4,  pos = pos, awaitSecrets = awaitSecret, maybeSecret = maybeSecret, delay = delay, center = center, stop = stop, chain = chain, reset) {

    override fun awaitTick(room: UniqueRoom) {
        PlayerUtils.unSneak()
        val yaw = room.getRealYaw(yaw)
        if (!silent) mc.thePlayer.rotationYaw = yaw
    }

    override fun tick(room: UniqueRoom) {
        PlayerUtils.unSneak()
        val yaw = room.getRealYaw(yaw)
        if (serverSneak) {
            Scheduler.schedulePreTickTask {
                startWalk(yaw)
            }
            return
        }
        startWalk(yaw)
    }



    override fun render(room: UniqueRoom) {
        Renderer.drawCylinder(room.getRealCoords(pos.add(Vec3(0.0, 0.03, 0.0))), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0,
            walkColor, depth = depth)
    }

    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("yaw", yaw)
    }

    override fun renderIndexColor(): Color {
        return  walkColor
    }

    override fun loadNodeInfo(obj: JsonObject) {
        yaw = obj.get("yaw").asFloat
    }


}