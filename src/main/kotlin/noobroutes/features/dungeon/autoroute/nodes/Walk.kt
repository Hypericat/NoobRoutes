package noobroutes.features.dungeon.autoroute.nodes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.dungeon.autoroute.AutoRoute.depth
import noobroutes.features.dungeon.autoroute.AutoRoute.serverSneak
import noobroutes.features.dungeon.autoroute.AutoRoute.silent
import noobroutes.features.dungeon.autoroute.Node
import noobroutes.utils.AutoP3Utils.startWalk
import noobroutes.utils.Scheduler
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
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
) : Node("Walk", 5,  pos = pos, awaitSecrets = awaitSecret, maybeSecret = maybeSecret, delay = delay, center = center, stop = stop, chain = chain) {

    override fun awaitTick(room: Room) {
        PlayerUtils.forceUnSneak()
        val yaw = room.getRealYaw(yaw)
        if (!silent) mc.thePlayer.rotationYaw = yaw
    }

    override fun tick(room: Room) {
        PlayerUtils.forceUnSneak()
        val yaw = room.getRealYaw(yaw)
        if (serverSneak) {
            Scheduler.schedulePreTickTask {
                startWalk(yaw)
            }
            return
        }
        startWalk(yaw)
    }



    override fun render(room: Room) {
        Renderer.drawCylinder(room.getRealCoords(pos.add(Vec3(0.0, 0.03, 0.0))), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0,
            AutoRoute.walkColor, depth = depth)
    }

    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("yaw", yaw)
    }


}