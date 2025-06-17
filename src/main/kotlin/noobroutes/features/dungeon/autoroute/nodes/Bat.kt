package noobroutes.features.dungeon.autoroute.nodes

import com.google.gson.JsonObject
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.dungeon.autoroute.AutoRoute.batColor
import noobroutes.features.dungeon.autoroute.AutoRoute.edgeRoutes
import noobroutes.features.dungeon.autoroute.AutoRouteUtils
import noobroutes.features.dungeon.autoroute.AutoRouteUtils.aotvTarget
import noobroutes.features.dungeon.autoroute.AutoRouteUtils.serverSneak
import noobroutes.features.dungeon.autoroute.Node
import noobroutes.features.dungeon.autoroute.SecretUtils
import noobroutes.utils.*
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.JsonUtils.asBlockPos
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealYaw
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage
import kotlin.math.absoluteValue

class Bat(
    pos: Vec3 = Vec3(0.0, 0.0, 0.0),
    var target: BlockPos? = null,
    var yaw: Float = 0f,
    var pitch: Float = 0f,
    awaitSecret: Int = 0,
    maybeSecret: Boolean = false,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
    reset: Boolean = false,
) : Node(
    "Bat",
    4,
    pos,
    awaitSecret,
    maybeSecret,
    delay,
    center,
    stop,
    chain,
    reset
) {


    override fun awaitMotion(event: MotionUpdateEvent.Pre, room: UniqueRoom) {
        AutoRouteUtils.setRotation(room.getRealYaw(yaw), pitch)
    }

    override fun motion(event: MotionUpdateEvent.Pre, room: UniqueRoom) {
        event.pitch = pitch
        event.yaw = room.getRealYaw(yaw)
    }

    override fun awaitTick(room: UniqueRoom) {
        PlayerUtils.unSneak()
    }

    override fun tick(room: UniqueRoom) {
        if (!AutoRoute.silent) RotationUtils.setAngles(room.getRealYaw(yaw), pitch)
        val state = SwapManager.swapFromSBId("HYPERION", "ASTRAEA", "VALKYRIE", "SCYLLA", "NECRON_BLADE")
        stopWalk()
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        if (state == SwapManager.SwapState.UNKNOWN) {
            return
        }

        val tpTarget = target?.let { room.getRealCoords(it) }

        if (mc.thePlayer.isSneaking || serverSneak || state != SwapManager.SwapState.ALREADY_HELD) {
            PlayerUtils.unSneak()
            AutoRouteUtils.setRotation(room.getRealYaw(yaw), pitch)
            Scheduler.schedulePreTickTask(1) {
                SecretUtils.batSpawnRegistered = true
                tpSetter(tpTarget, room)
                aotvTarget = tpTarget
            }
            return
        }
        SecretUtils.batSpawnRegistered = true
        tpSetter(tpTarget, room)
        aotvTarget = tpTarget
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
                target = room.getRelativeCoords(BlockPos(s08.x, s08.y, s08.z))
            }
        }
    }


    override fun render(room: UniqueRoom) {
        drawNode(room, batColor)
        if (!AutoRoute.drawAotvLines) return
        val targetCoords = target?.let { room.getRealCoords(it) } ?: return
        val nodePosition = room.getRealCoords(pos)
        val yaw = room.getRealYaw(yaw)
        if (edgeRoutes && pitch.absoluteValue != 90f) {
            Renderer.draw3DLine(
                listOf(
                    nodePosition.add(yaw.xPart * 0.6, 0.0, yaw.zPart * 0.6),
                    targetCoords.toVec3().add(0.5, 0.0, 0.5),
                ),
                batColor,
                depth = AutoRoute.depth
            )
        } else {
            Renderer.draw3DLine(
                listOf(
                    nodePosition,
                    targetCoords.toVec3().add(0.5, 0.0, 0.5)
                ),
                batColor,
                depth = AutoRoute.depth
            )
        }
    }

    override fun nodeAddInfo(obj: JsonObject) {
        target?.let { obj.addProperty("target", it) }
        obj.addProperty("yaw", yaw)
        obj.addProperty("pitch", pitch)
    }

    override fun loadNodeInfo(obj: JsonObject) {
        this.target = obj.get("target")?.asBlockPos ?: BlockPos(0.0, 0.0, 0.0)
        this.yaw = obj.get("yaw").asFloat
        this.pitch = obj.get("pitch").asFloat
    }

    override fun renderIndexColor(): Color {
        return  batColor
    }
}