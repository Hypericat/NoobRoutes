package noobroutes.features.routes.nodes.autoroutes

import com.google.gson.JsonObject
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.routes.AutoRoute
import noobroutes.features.routes.nodes.AutoRouteNodeBase
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.features.routes.nodes.NodeType
import noobroutes.utils.*
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.JsonUtils.asBlockPos
import noobroutes.utils.render.Color
import noobroutes.utils.routes.RouteUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage

class Boom(
    pos: Vec3,
    var target: BlockPos,
    base: AutoRouteNodeBase
) : AutorouteNode(
    pos,
    base
) {
    companion object : NodeLoader {
        override fun loadNodeInfo(obj: JsonObject): AutorouteNode {
            val base = getBaseFromObj(obj)
            val target = obj.get("target").asBlockPos
            return Boom(
                obj.getCoords(),
                target,
                base
            )
        }

        override fun generateFromArgs(
            args: Array<out String>,
            room: UniqueRoom
        ): AutorouteNode? {
            val block = mc.objectMouseOver.blockPos
            if (isAir(block)) {
                modMessage("must look at a block")
                return null
            }
            val base = getBaseFromArgs( args)
            return Boom(
                getCoords(room),
                room.getRelativeCoords(block),
                base
            )
        }
    }

    override val priority: Int = 9

    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("target", target)
    }



    override fun updateTick() {
        // L loser
        // I am not artistic, I'm autistic - wadey
        return
    }

    override fun run() {
        val room = currentRoom ?: return

        val pos = room.getRealCoords(target)
        if (isAir(pos)) {
            return
        }

        val state = SwapManager.swapFromSBId("INFINITE_SUPERBOOM_TNT", "SUPERBOOM_TNT")
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (!AutoRoute.silent) {
            val angles = RotationUtils.getYawAndPitch(pos.toVec3().add(0.5, 0.5, 0.5))
            RotationUtils.setAngles(angles.first, angles.second)
        }
        if (state == SwapManager.SwapState.SWAPPED) {
            AutoRoute.delay = System.currentTimeMillis() + 200
            Scheduler.schedulePreTickTask {
                RouteUtils.lastRoute = System.currentTimeMillis()
                if (!isAir(pos)) {
                    AuraManager.auraBlock(pos, true)
                }
            }
            return
        }
        AutoRoute.delay = System.currentTimeMillis() + 150
        if (!isAir(pos)) {
            AuraManager.auraBlock(pos, true)
        }

    }

    override fun getRenderColor(): Color {
        return AutoRoute.boomColor
    }

    override fun getType(): NodeType {
        return NodeType.BOOM
    }




}
