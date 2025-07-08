package noobroutes.features.routes.nodes.autoroutes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.routes.AutoRoute
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.features.routes.nodes.NodeType
import noobroutes.utils.AutoP3Utils.startWalk
import noobroutes.utils.Scheduler
import noobroutes.utils.render.Color
import noobroutes.utils.round
import noobroutes.utils.routes.RouteUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealYaw
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeYaw
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom

class Walk(
    pos: Vec3,
    var yaw: Float,
    awaitSecrets: Int = 0,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
    reset: Boolean = false,
) : AutorouteNode(
    pos,
    awaitSecrets,
    delay,
    center,
    stop,
    chain,
    reset
) {
    companion object : NodeLoader {
        override fun loadNodeInfo(obj: JsonObject): AutorouteNode {
            val general =  getGeneralNodeArgsFromObj(obj)
            val yaw = obj.get("yaw").asFloat

            return Walk(
                general.pos,
                yaw,
                general.awaitSecrets,
                general.delay,
                general.center,
                general.stop,
                general.chain,
                general.reset,
            )
        }

        override fun generateFromArgs(
            args: Array<out String>,
            room: UniqueRoom
        ): AutorouteNode? {
            val generalNodeArgs = getGeneralNodeArgs(room, args)
            return Walk(
                generalNodeArgs.pos,
                room.getRelativeYaw(mc.thePlayer.rotationYaw.round(14).toFloat()),
                generalNodeArgs.awaitSecrets,
                generalNodeArgs.delay,
                generalNodeArgs.center,
                generalNodeArgs.stop,
                generalNodeArgs.chain,
                generalNodeArgs.reset
            )
        }
    }


    override val priority: Int = 4
    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("yaw", yaw)
    }



    override fun updateTick() {
        val room = currentRoom ?: return
        PlayerUtils.unSneak()
        val yaw = room.getRealYaw(yaw)
        if (!isSilent()) mc.thePlayer.rotationYaw = yaw
    }

    override fun run() {
        val room = currentRoom ?: return

        PlayerUtils.unSneak()
        val yaw = room.getRealYaw(yaw)
        if (RouteUtils.serverSneak) {
            Scheduler.schedulePreTickTask {
                startWalk(yaw)
            }
            return
        }
        startWalk(yaw)
    }

    override fun getType(): NodeType {
        return NodeType.WALK
    }


    override fun getRenderColor(): Color {
        return AutoRoute.walkColor
    }


}