package noobroutes.features.routes.nodes.autoroutes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.AutoP3MovementHandler
import noobroutes.features.routes.AutoRoute
import noobroutes.features.routes.nodes.AutoRouteNodeBase
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.features.routes.nodes.NodeType
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
    base: AutoRouteNodeBase
) : AutorouteNode(
    pos,
    base
) {
    companion object : NodeLoader {
        override fun loadNodeInfo(obj: JsonObject): AutorouteNode {
            val base = getBaseFromObj(obj)
            val yaw = obj.get("yaw").asFloat

            return Walk(
                obj.getCoords(),
                yaw,
                base
            )
        }

        override fun generateFromArgs(
            args: Array<out String>,
            room: UniqueRoom
        ): AutorouteNode? {
            val base = getBaseFromArgs(args)
            return Walk(
                getCoords(room),
                room.getRelativeYaw(mc.thePlayer.rotationYaw.round(14).toFloat()),
                base
            )
        }
    }


    override val priority: Int = 4
    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("yaw", yaw)
    }



    override fun updateTick() {
        val room = currentRoom ?: return
        PlayerUtils.unSneak(isSilent())
        val yaw = room.getRealYaw(yaw)
        if (!isSilent()) mc.thePlayer.rotationYaw = yaw
    }

    override fun run() {
        val room = currentRoom ?: return

        PlayerUtils.unSneak(isSilent())
        val yaw = room.getRealYaw(yaw)
        if (RouteUtils.serverSneak) {
            Scheduler.schedulePreTickTask {
                AutoP3MovementHandler.setDirection(yaw)
            }
            return
        }
        AutoP3MovementHandler.setDirection(yaw)
    }

    override fun getType(): NodeType {
        return NodeType.WALK
    }


    override fun getRenderColor(): Color {
        return AutoRoute.walkColor
    }


}