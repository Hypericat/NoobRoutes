package noobroutes.features.routes.nodes.autoroutes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.features.routes.AutoRoute
import noobroutes.features.routes.nodes.AutoRouteNodeBase
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.features.routes.nodes.NodeType
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.routes.RouteUtils
import noobroutes.utils.routes.RouteUtils.pearlClip
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage
import kotlin.math.absoluteValue

class PearlClip(
    pos: Vec3,
    var distance: Int,
    base: AutoRouteNodeBase
): AutorouteNode(
    pos,
    base
) {
    companion object : NodeLoader {
        override fun loadNodeInfo(obj: JsonObject): AutorouteNode {
            val distance = obj.get("distance").asInt
            val base = getBaseFromObj(obj)
            return PearlClip(
                obj.getCoords(),
                distance,
                base
            )
        }

        override fun generateFromArgs(
            args: Array<out String>,
            room: UniqueRoom
        ): AutorouteNode? {
            if (args.size < 3) {
                modMessage("Need Distance")
                return null
            }
            val distance = args[2].toIntOrNull()?.absoluteValue?.minus(if (AutoRoute.decrease) 1 else 0)
            if (distance == null) {
                modMessage("Provide a Number thanks")
                return null
            }
            if (distance < 1) {
                modMessage("Invalid Number, has to be greater than 0")
                return null
            }
            val base = getBaseFromArgs(args)
            return PearlClip(
                getCoords(room),
                distance,
                base
            )
        }
    }



    override val priority: Int = 7
    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("distance", distance)
    }



    override fun updateTick() {
        RouteUtils.setRotation(null, 90f, isSilent())
    }

    override fun run() {
        pearlClip(distance, isSilent())
    }

    override fun getType(): NodeType {
        return NodeType.PEARL_CLIP
    }

    override fun getRenderColor(): Color {
        return AutoRoute.pearlClipColor
    }

    override fun render() {
        val room = currentRoom ?: return
        Renderer.drawCylinder(room.getRealCoords(pos.add(Vec3(0.0, 0.03, 0.0))), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, getRenderColor(), getDepth())
        if (AutoRoute.drawPearlClipText) Renderer.drawStringInWorld("PearlClip: $distance", room.getRealCoords(pos).add(Vec3(0.0, 0.9, 0.0)), getRenderColor(), depth = getDepth())
    }

}