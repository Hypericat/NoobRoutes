package noobroutes.features.dungeon.autoroute.nodes

import com.google.gson.JsonObject
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.dungeon.autoroute.AutoRoute.aotvColor
import noobroutes.features.dungeon.autoroute.AutoRoute.edgeRoutes
import noobroutes.features.dungeon.autoroute.AutoRoute.serverSneak
import noobroutes.features.dungeon.autoroute.Node
import noobroutes.features.move.Zpew
import noobroutes.utils.*
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.JsonUtils.asBlockPos
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealYaw
import noobroutes.utils.skyblock.dungeon.tiles.Room
import noobroutes.utils.skyblock.modMessage

class Aotv(
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
) : Node(
    "Aotv",
    5,
    pos,
    awaitSecret,
    maybeSecret,
    delay,
    center,
    stop,
    chain
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
        super.tick(room)
        if (!AutoRoute.silent) RotationUtils.setAngles(room.getRealYaw(yaw), pitch)
        val state = SwapManager.swapFromSBId("ASPECT_OF_THE_VOID")
        stopWalk()
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        val tpTarget = room.getRealCoords(target)

        if (mc.thePlayer.isSneaking || serverSneak || state != SwapManager.SwapState.ALREADY_HELD) {
            PlayerUtils.forceUnSneak()
            AutoRoute.rotatingPitch = pitch
            AutoRoute.rotatingYaw = room.getRealYaw(yaw)
            AutoRoute.rotating = true
            Scheduler.schedulePreTickTask(1) {
                AutoRoute.aotv(tpTarget)
            }
            return
        }
        AutoRoute.aotv(tpTarget)


    }


    override fun render(room: Room) {
        drawNode(room, aotvColor)
        if (edgeRoutes) {
            val targetCoords = room.getRealCoords(target)
            val yaw = RotationUtils.getYawAndPitchOrigin(room.getRealCoords(pos), targetCoords.toVec3(), true).first
            Renderer.draw3DLine(
                listOf(
                    room.getRealCoords(pos).add(yaw.xPart * 0.6, 0.0, yaw.zPart * 0.6),
                    targetCoords.toVec3().add(0.5, 0.0, 0.5),
                ),
                aotvColor,
                depth = AutoRoute.depth
            )
        } else {
            Renderer.draw3DLine(
                listOf(
                    room.getRealCoords(pos),
                    room.getRealCoords(target).toVec3().add(0.5, 0.0, 0.5)
                ),
                aotvColor,
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
}