package noobroutes.features.routes.nodes.autoroutes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.features.routes.nodes.NodeType
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.Core.mc
import noobroutes.features.routes.nodes.AutoRouteNodeBase
import noobroutes.utils.Scheduler
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart
import noobroutes.utils.roundToNearest
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealYaw

class BlockClip(
    pos: Vec3,
    val yaw: Float,
    base: AutoRouteNodeBase
) : AutorouteNode(
    pos,
    base
) {
    companion object : NodeLoader {
        const val INITIAL_DISTANCE = 0.262
        const val SECOND_DISTANCE = 2.0

        override fun loadNodeInfo(obj: JsonObject): AutorouteNode {
            val base = getBaseFromObj(obj)
            val yaw = obj.get("yaw").asFloat
            return BlockClip(obj.getCoords(), yaw, base)
        }

        override fun generateFromArgs(
            args: Array<out String>,
            room: UniqueRoom
        ): AutorouteNode? {
            val base = getBaseFromArgs(args)
            val yaw = (mc.thePlayer.rotationYaw + 180).roundToNearest(90f, 180f, 270f, 360f)
            return BlockClip(getCoords(room), yaw, base)
        }

    }

    override val priority: Int = 4
    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("yaw", yaw)
    }

    override fun updateTick() {}

    override fun run() {
        val room = currentRoom ?: return
        val realCoords = room.getRealCoords(pos)
        val realYaw = room.getRealYaw(yaw)
        PlayerUtils.setPosition(realCoords.xCoord + realYaw.xPart * INITIAL_DISTANCE, realCoords.zCoord + realYaw.zPart * INITIAL_DISTANCE)
        Scheduler.scheduleHighPreTickTask {
            PlayerUtils.setPosition(realCoords.xCoord + realYaw.xPart * SECOND_DISTANCE, realCoords.zCoord + realYaw.zPart * SECOND_DISTANCE)
        }
    }

    override fun getType(): NodeType {
        return NodeType.BLOCK_CLIP
    }


}