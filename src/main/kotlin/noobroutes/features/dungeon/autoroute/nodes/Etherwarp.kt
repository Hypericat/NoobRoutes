package noobroutes.features.dungeon.autoroute.nodes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.dungeon.autoroute.AutoRoute.depth
import noobroutes.features.dungeon.autoroute.AutoRoute.edgeRoutes
import noobroutes.features.dungeon.autoroute.AutoRoute.ether
import noobroutes.features.dungeon.autoroute.AutoRoute.silent
import noobroutes.features.dungeon.autoroute.Node
import noobroutes.utils.RotationUtils
import noobroutes.utils.RotationUtils.setAngles
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart
import noobroutes.utils.add
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.Room
import noobroutes.utils.skyblock.skyblockID

class Etherwarp(
    pos: Vec3,
    var target: Vec3,
    awaitSecret: Int = 0,
    maybeSecret: Boolean = false,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
) : Node(
    "Etherwarp",
    2,
    pos,
    awaitSecret,
    maybeSecret,
    delay,
    center,
    stop,
    chain
) {

    override fun awaitTick(room: Room) {
        SwapManager.swapFromSBId("ASPECT_OF_THE_VOID")
        val angles = RotationUtils.getYawAndPitch(room.getRealCoords(target))
        if (!silent) setAngles(angles.first, angles.second)
    }

    override fun awaitMotion(event: MotionUpdateEvent.Pre, room: Room) {
        val angles = RotationUtils.getYawAndPitch(room.getRealCoords(target))
        AutoRoute.rotatingYaw = angles.first
        AutoRoute.rotatingPitch = angles.second
        AutoRoute.rotating = true
    }

    override fun tick(room: Room) {
        devMessage("run tick:${System.currentTimeMillis()}")
        val angles = RotationUtils.getYawAndPitch(room.getRealCoords(target))
        if (stop) PlayerUtils.stopVelocity()
        if (center) center()
        SwapManager.swapFromSBId("ASPECT_OF_THE_VOID")
        if (!silent) setAngles(angles.first, angles.second)
        stopWalk()
    }


    override fun motion(event: MotionUpdateEvent.Pre, room: Room) {
        devMessage("run motion:${System.currentTimeMillis()}")
        val angles = RotationUtils.getYawAndPitch(room.getRealCoords(target))
        event.yaw = angles.first
        event.pitch = angles.second
        if (!mc.thePlayer.isSneaking) {
            AutoRoute.rotatingYaw = angles.first
            AutoRoute.rotatingPitch = angles.second
            AutoRoute.rotating = true
        }
        if (mc.thePlayer.heldItem.skyblockID != "ASPECT_OF_THE_VOID") {
            AutoRoute.rotatingYaw = angles.first
            AutoRoute.rotatingPitch = angles.second
            AutoRoute.rotating = true
            Scheduler.schedulePreTickTask {
                ether()
            }
        } else ether()
    }


    override fun render(room: Room) {
        val nodeCoords = room.getRealCoords(pos)
        Renderer.drawCylinder(nodeCoords.add(0.0, 0.03, 0.0), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0,
            AutoRoute.etherwarpColor , depth = depth)
        if (edgeRoutes) {
            val targetCoords = room.getRealCoords(target)
            val yaw = RotationUtils.getYawAndPitchOrigin(nodeCoords, targetCoords, true).first
            Renderer.draw3DLine(
                listOf(
                    room.getRealCoords(pos).add(yaw.xPart * 0.6, 0.0, yaw.zPart * 0.6),
                    targetCoords,
                ),
                AutoRoute.etherwarpColor
            )
        } else {
            Renderer.draw3DLine(
                listOf(
                    room.getRealCoords(pos),
                    room.getRealCoords(target)
                ),
                AutoRoute.etherwarpColor
            )
        }


    }

    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("target", target)
    }
}