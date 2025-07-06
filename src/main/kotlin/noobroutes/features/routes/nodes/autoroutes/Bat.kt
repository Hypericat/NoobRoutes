package noobroutes.features.routes.nodes.autoroutes

import com.google.gson.JsonObject
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.events.impl.PacketEvent
import noobroutes.features.routes.AutoRoute
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.features.routes.nodes.NodeType
import noobroutes.utils.*
import noobroutes.utils.RotationUtils.offset
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.JsonUtils.asBlockPos
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.routes.RouteUtils
import noobroutes.utils.routes.SecretUtils
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealYaw
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage
import kotlin.math.absoluteValue

class Bat(
    pos: Vec3,
    var target: BlockPos?,
    val yaw : Float,
    val pitch: Float,
    awaitSecrets: Int = 0,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
    reset: Boolean = false

) : AutorouteNode(
    pos,
    awaitSecrets,
    delay,
    center,
    stop,
    chain,
    reset
) {
    override val priority: Int = 4

    companion object : NodeLoader {
        override fun loadNodeInfo(obj: JsonObject): AutorouteNode {
            val target = obj.get("target")?.asBlockPos
            val yaw = obj.get("yaw").asFloat
            val pitch = obj.get("pitch").asFloat
            val general = getGeneralNodeArgsFromObj(obj)
            return Bat(
                general.pos,
                target,
                yaw,
                pitch,
                general.awaitSecrets,
                general.delay,
                general.center,
                general.stop,
                general.chain,
                general.reset
            )
        }

        override fun generateFromArgs(
            args: Array<out String>,
            room: UniqueRoom
        ): AutorouteNode? {
            return null
        }
    }

    override fun nodeAddInfo(obj: JsonObject) {
        target?.let { obj.addProperty("target", it) }
        obj.addProperty("yaw", yaw)
        obj.addProperty("pitch", pitch)
    }



    override fun updateTick() {
        val room = currentRoom ?: return
        PlayerUtils.unSneak()
        RouteUtils.setRotation(room.getRealYaw(yaw),pitch + offset, AutoRoute.silent)
    }

    override fun run() {
        val room = currentRoom ?: return
        val state = if (LocationUtils.isSinglePlayer) SwapManager.swapFromId(267) else SwapManager.swapFromSBId("HYPERION", "ASTRAEA", "VALKYRIE", "SCYLLA", "NECRON_BLADE")
        stopWalk()
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        if (state == SwapManager.SwapState.UNKNOWN) {
            return
        }
        PlayerUtils.sneak()
        RouteUtils.setRotation(room.getRealYaw(yaw), pitch, AutoRoute.silent)
        val tpTarget = target?.let { room.getRealCoords(it) }
        SecretUtils.batSpawnRegistered = true
        tpSetter(tpTarget, room)
        RouteUtils.aotvTarget = tpTarget
    }

    fun tpSetter(tpTarget: BlockPos?, room: UniqueRoom){
        if (tpTarget == null) {
            val timeClicked = System.currentTimeMillis()
            Scheduler.scheduleLowS08Task {
                if (timeClicked + 5000 < System.currentTimeMillis()) {
                    modMessage("recording timed out")
                    return@scheduleLowS08Task
                }
                val event = (it as? PacketEvent.Receive) ?: return@scheduleLowS08Task
                val s08 = event.packet as S08PacketPlayerPosLook
                val flag = s08.func_179834_f()
                if (
                    flag.contains(S08PacketPlayerPosLook.EnumFlags.X) ||
                    flag.contains(S08PacketPlayerPosLook.EnumFlags.Y) ||
                    flag.contains(S08PacketPlayerPosLook.EnumFlags.Z) ||
                    event.isCanceled ||
                    s08.y - s08.y.floor() != 0.0
                ) {
                    modMessage("Invalid Packet")
                    return@scheduleLowS08Task
                }
                this.target = room.getRelativeCoords(BlockPos(s08.x, s08.y, s08.z))
            }
        }
    }


    override fun getRenderColor(): Color {
        return AutoRoute.batColor
    }

    override fun render() {
        val room = currentRoom ?: return
        super.render()

        if (!AutoRoute.drawAotvLines) return
        val targetCoords = target?.let { room.getRealCoords(it) } ?: return
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
        return NodeType.BAT
    }
}