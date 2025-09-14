package noobroutes.features.routes.nodes.autoroutes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.routes.AutoRoute
import noobroutes.features.routes.nodes.AutoRouteNodeBase
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.features.routes.nodes.NodeType
import noobroutes.utils.RotationUtils
import noobroutes.utils.SwapManager
import noobroutes.utils.render.Color
import noobroutes.utils.routes.RouteUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealYaw
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeYaw
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage

class UseItem(
    pos: Vec3,
    var itemName: String,
    var yaw: Float,
    var pitch: Float,
    base: AutoRouteNodeBase
) : AutorouteNode(
    pos,
    base
) {
    companion object : NodeLoader {
        override fun loadNodeInfo(obj: JsonObject): AutorouteNode {
            val base = getBaseFromObj(obj)
            val yaw = obj.get("yaw").asFloat
            val pitch = obj.get("pitch").asFloat
            val itemName = obj.get("itemName").asString

            return UseItem(
                obj.getCoords(),
                itemName,
                yaw,
                pitch,
                base
            )
        }

        override fun generateFromArgs(
            args: Array<out String>,
            room: UniqueRoom
        ): AutorouteNode? {
            if (args.size < 3) {
                modMessage("Need Item Name")
                return null
            }
            val base = getBaseFromArgs(args)
            val name = args[2].toString()
            val yaw = room.getRelativeYaw(mc.thePlayer.rotationYaw)
            val pitch = mc.thePlayer.rotationPitch
            return UseItem(
                getCoords(room),
                name,
                yaw,
                pitch,
                base
            )
        }
    }

    override val priority: Int = 5
    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("itemName", itemName)
        obj.addProperty("yaw", yaw)
        obj.addProperty("pitch", pitch)
    }



    override fun updateTick() {
        val room = currentRoom ?: return
        PlayerUtils.unSneak(isSilent())
        RouteUtils.setRotation(room.getRealYaw(yaw), pitch + RotationUtils.offset, isSilent())
    }

    override fun run() {
        val room = currentRoom ?: return

        val state = SwapManager.swapFromName(itemName)
        stopWalk()
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        RouteUtils.setRotation(room.getRealYaw(yaw), pitch, AutoRoute.silent)
        RouteUtils.unsneak(AutoRoute.silent)
    }


    override fun getRenderColor(): Color {
        return AutoRoute.useItemColor
    }

    override fun getType(): NodeType {
        return NodeType.USE_ITEM
    }
}