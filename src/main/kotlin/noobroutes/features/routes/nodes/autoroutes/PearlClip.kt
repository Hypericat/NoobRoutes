package noobroutes.features.routes.nodes.autoroutes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.features.routes.AutoRoute
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.features.routes.nodes.NodeType
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.routes.RouteUtils
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.sendChatMessage
import kotlin.math.absoluteValue

class PearlClip(
    pos: Vec3,
    var distance: Int,
    awaitSecrets: Int = 0,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
    reset: Boolean = false,
): AutorouteNode(
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
            val distance = obj.get("distance").asInt
            val pos = obj.get("position").asVec3
            val awaitSecrets = obj.get("secrets")?.asInt ?: 0
            val delay = obj.get("delay")?.asLong ?: 0L
            val center = obj.has("center")
            val stop = obj.has("stop")
            val chain = obj.has("chain")
            val reset = obj.has("reset")
            return PearlClip(pos, distance, awaitSecrets, delay, center, stop, chain, reset)
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
            val generalNodeArgs = getGeneralNodeArgs(room, args)
            return PearlClip(
                generalNodeArgs.pos,
                distance,
                generalNodeArgs.awaitSecrets,
                generalNodeArgs.delay,
                generalNodeArgs.center,
                generalNodeArgs.stop,
                generalNodeArgs.chain,
                generalNodeArgs.reset
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
        if (distance > 70) return modMessage("Invalid Clip Distance")
        stopWalk()
        val state = SwapManager.swapFromName("ender pearl")
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        RouteUtils.clipDistance = distance
        if (LocationUtils.isSinglePlayer) {
            Scheduler.schedulePreTickTask(1) { sendChatMessage("/tp ~ ~-$distance ~") }
        }
        RouteUtils.pearlSoundRegistered = true
        RouteUtils.setRotation(null, 90f, isSilent())
        RouteUtils.rightClick()
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