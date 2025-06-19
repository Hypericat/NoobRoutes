package noobroutes.features.routes.nodes.autoroutes

import com.google.gson.JsonObject
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.features.routes.AutoRoute
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.features.routes.nodes.NodeType
import noobroutes.utils.AuraManager
import noobroutes.utils.RotationUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.add
import noobroutes.utils.isAir
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.JsonUtils.asBlockPos
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.render.Color
import noobroutes.utils.routes.RouteUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.toVec3

class Boom(
    pos: Vec3,
    var target: BlockPos,
    awaitSecret: Int = 0,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
    reset: Boolean = false,
) : NodeLoader, AutorouteNode(
    pos,
    awaitSecret,
    delay,
    center,
    stop,
    chain,
    reset
) {
    override val priority: Int = 9

    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("target", target)
    }

    override fun loadNodeInfo(obj: JsonObject): AutorouteNode {
        val pos = obj.get("position").asVec3
        val center = obj.has("center")
        val chain = obj.has("chain")
        val reset = obj.has("reset")
        val stop = obj.has("stop")
        val awaitSecrets = obj.get("secrets")?.asInt ?: 0
        val delay = obj.get("delay")?.asLong ?: 0L
        val target = obj.get("target").asBlockPos
        return Boom(pos, target, awaitSecrets, delay, center, stop, chain, reset)
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
            AutoRoute.delay = System.currentTimeMillis() + 350
            Scheduler.schedulePreTickTask {
                RouteUtils.lastRoute = System.currentTimeMillis()
                if (!isAir(pos)) {
                    AuraManager.auraBlock(pos, true)
                }
            }
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
