package noobroutes.features.routes.nodes.autoroutes

import com.google.gson.JsonObject
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.features.routes.AutoRoute
import noobroutes.utils.routes.RouteUtils
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.features.routes.nodes.NodeType
import noobroutes.utils.RotationUtils.offset
import noobroutes.utils.SwapManager
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart
import noobroutes.utils.add
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.JsonUtils.asBlockPos
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealYaw
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.toVec3
import kotlin.math.absoluteValue

class Aotv(
    pos: Vec3,
    val target: BlockPos,
    val yaw : Float,
    val pitch: Float,
    awaitSecrets: Int = 0,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
    reset: Boolean = false

) : NodeLoader, AutorouteNode(
    pos,
    awaitSecrets,
    delay,
    center,
    stop,
    chain,
    reset
) {
    override val priority: Int = 5

    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("target", target)
        obj.addProperty("yaw", yaw)
        obj.addProperty("pitch", pitch)
    }

    override fun loadNodeInfo(obj: JsonObject): AutorouteNode {
        val pos = obj.get("position").asVec3
        val target = obj.get("target").asBlockPos
        val yaw = obj.get("yaw").asFloat
        val pitch = obj.get("pitch").asFloat
        val awaitSecrets = obj.get("secrets")?.asInt ?: 0
        val delay = obj.get("delay")?.asLong ?: 0L
        val center = obj.has("center")
        val stop = obj.has("stop")
        val chain = obj.has("chain")
        val reset = obj.has("reset")

        return Aotv(pos, target, yaw, pitch, awaitSecrets, delay, center, stop, chain, reset)
    }

    override fun generateFromArgs(
        args: Array<out String>,
        room: UniqueRoom
    ): AutorouteNode? {
        
    }

    override fun updateTick() {
        val room = currentRoom ?: return
        PlayerUtils.unSneak()
        RouteUtils.setRotation(room.getRealYaw(yaw), pitch + offset, AutoRoute.silent)
    }

    override fun run() {
        val room = currentRoom ?: return
        val state = SwapManager.swapFromSBId("ASPECT_OF_THE_VOID")
        stopWalk()
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        val tpTarget = target.let { room.getRealCoords(it) }
        PlayerUtils.sneak()
        RouteUtils.setRotation(room.getRealYaw(yaw), pitch, isSilent())
        RouteUtils.aotv(tpTarget)
    }

    override fun getRenderColor(): Color {
        return AutoRoute.aotvColor
    }

    override fun render() {
        val room = currentRoom ?: return
        super.render()

        if (!AutoRoute.drawAotvLines) return
        val targetCoords = room.getRealCoords(target)
        val nodePosition = room.getRealCoords(pos)
        val yaw = room.getRealYaw(yaw)
        if (AutoRoute.edgeRoutes && pitch.absoluteValue != 90f) {
            Renderer.draw3DLine(
                listOf(
                    nodePosition.add(yaw.xPart * 0.6, 0.0, yaw.zPart * 0.6),
                    targetCoords.toVec3().add(0.5, 0.0, 0.5),
                ),
                getRenderColor(),
                depth = getDepth()
            )
        } else {
            Renderer.draw3DLine(
                listOf(
                    nodePosition,
                    targetCoords.toVec3().add(0.5, 0.0, 0.5)
                ),
                getRenderColor(),
                depth = getDepth()
            )
        }
    }

    override fun getType(): NodeType {
        return NodeType.AOTV
    }
}