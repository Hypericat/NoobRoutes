package noobroutes.features.dungeon.autoroute.nodes

import com.google.gson.JsonObject
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.dungeon.autoroute.AutoRoute.aotvTarget
import noobroutes.features.dungeon.autoroute.AutoRoute.batColor
import noobroutes.features.dungeon.autoroute.AutoRoute.edgeRoutes
import noobroutes.features.dungeon.autoroute.AutoRoute.serverSneak
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
import noobroutes.utils.skyblock.dungeon.tiles.Room
import noobroutes.utils.skyblock.modMessage
import kotlin.math.absoluteValue

class Bat(
    pos: Vec3 = Vec3(0.0, 0.0, 0.0),
    var target: BlockPos = BlockPos(0,0,0),
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

    override fun awaitMotion(event: MotionUpdateEvent.Pre, room: Room) {
        AutoRoute.rotatingPitch = pitch
        AutoRoute.rotatingYaw = room.getRealYaw(yaw)
        AutoRoute.rotating = true
    }

    override fun motion(event: MotionUpdateEvent.Pre, room: Room) {
        event.pitch = pitch
        event.yaw = room.getRealYaw(yaw)
    }

    override fun awaitTick(room: Room) {
        PlayerUtils.forceUnSneak()
    }

    override fun tick(room: Room) {
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

        val tpTarget = room.getRealCoords(target)

        if (mc.thePlayer.isSneaking || serverSneak || state != SwapManager.SwapState.ALREADY_HELD) {
            PlayerUtils.forceUnSneak()
            AutoRoute.rotatingPitch = pitch
            AutoRoute.rotatingYaw = room.getRealYaw(yaw)
            AutoRoute.rotating = true
            Scheduler.schedulePreTickTask(1) {
                SecretUtils.batSpawnRegistered = true
                aotvTarget = tpTarget
            }
            return
        }
        SecretUtils.batSpawnRegistered = true
        aotvTarget = tpTarget


    }


    override fun render(room: Room) {
        drawNode(room, batColor)
        if (!AutoRoute.drawAotvLines) return
        val targetCoords = room.getRealCoords(target)
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
        obj.addProperty("target", target)
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