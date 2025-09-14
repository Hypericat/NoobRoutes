package noobroutes.features.routes.nodes.autoroutes

import com.google.gson.JsonObject
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.routes.nodes.AutoRouteNodeBase
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.features.routes.nodes.NodeType
import noobroutes.utils.Scheduler
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart
import noobroutes.utils.requirement
import noobroutes.utils.roundToNearest
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealYaw
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeYaw
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage

class Clip(pos: Vec3, val distance: Double, val yaw: Float, base: AutoRouteNodeBase) : AutorouteNode(pos, base) {
    companion object : NodeLoader {
        override fun loadNodeInfo(obj: JsonObject): AutorouteNode {
            val base = getBaseFromObj(obj)
            val yaw = obj.get("yaw").asFloat
            val distance = obj.get("distance").asDouble
            return Clip(obj.getCoords(), distance, yaw, base)
        }

        override fun generateFromArgs(
            args: Array<out String>,
            room: UniqueRoom
        ): AutorouteNode? {
            if (!args.requirement(3)) {
                modMessage("requires distance arg")
                return null
            }
            val distance = args[2].toDoubleOrNull() ?: run {
                modMessage("invalid distance")
                return null
            }
            val base = getBaseFromArgs(args)

            return Clip(getCoords(room), distance, room.getRelativeYaw(mc.thePlayer.rotationYaw), base)
        }

    }

    override val priority: Int = 4
    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("yaw", yaw)
        obj.addProperty("distance", distance)
    }

    override fun updateTick() {
        PlayerUtils.unSneak(isSilent())
    }

    override fun run() {
        val room = currentRoom ?: return
        val realCoords = room.getRealCoords(pos)
        val realYaw = room.getRealYaw(yaw)
        PlayerUtils.unSneak(isSilent())
        PlayerUtils.setPosition(realCoords.xCoord + realYaw.xPart * distance, realCoords.zCoord + realYaw.zPart * distance)
        Scheduler.scheduleC03Task {
            PlayerUtils.resyncSneak()
        }
    }

    override fun getType(): NodeType {
        return NodeType.CLIP
    }



}