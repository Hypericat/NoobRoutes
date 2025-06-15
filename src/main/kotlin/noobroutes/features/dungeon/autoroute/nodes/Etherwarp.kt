package noobroutes.features.dungeon.autoroute.nodes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.dungeon.autoroute.AutoRoute.depth
import noobroutes.features.dungeon.autoroute.AutoRoute.edgeRoutes
import noobroutes.features.dungeon.autoroute.AutoRoute.etherwarpColor
import noobroutes.features.dungeon.autoroute.AutoRoute.silent
import noobroutes.features.dungeon.autoroute.AutoRouteUtils
import noobroutes.features.dungeon.autoroute.AutoRouteUtils.ether
import noobroutes.features.dungeon.autoroute.Node
import noobroutes.utils.RotationUtils
import noobroutes.utils.RotationUtils.offset
import noobroutes.utils.RotationUtils.setAngles
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart
import noobroutes.utils.add
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.Room
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.skyblockID
import kotlin.math.absoluteValue

class Etherwarp(
    pos: Vec3 = Vec3(0.0, 0.0, 0.0),
    var target: Vec3 = Vec3(0.0, 0.0, 0.0),
    awaitSecret: Int = 0,
    maybeSecret: Boolean = false,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
    reset: Boolean = false,
) : Node(
    "Etherwarp",
    8,
    pos,
    awaitSecret,
    maybeSecret,
    delay,
    center,
    stop,
    chain,
    reset
) {

    override fun awaitTick(room: Room) {
        val angles = RotationUtils.getYawAndPitch(room.getRealCoords(target))
        if (!silent) setAngles(angles.first, angles.second)
    }

    override fun awaitMotion(event: MotionUpdateEvent.Pre, room: Room) {
        val angles = RotationUtils.getYawAndPitch(room.getRealCoords(target))
        devMessage("yaw: ${angles.first}, pitch: ${angles.second}")
        AutoRouteUtils.setRotation(angles.first + offset,angles.second)
    }

    override fun tick(room: Room) {
        val angles = RotationUtils.getYawAndPitch(room.getRealCoords(target))
        val state = SwapManager.swapFromSBId("ASPECT_OF_THE_VOID")
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        if (!silent) setAngles(angles.first, angles.second)
        stopWalk()
        PlayerUtils.sneak()
    }


    override fun motion(event: MotionUpdateEvent.Pre, room: Room) {
        val angles = RotationUtils.getYawAndPitch(room.getRealCoords(target))
        event.yaw = angles.first
        event.pitch = angles.second
        if (!mc.thePlayer.isSneaking || mc.thePlayer.heldItem.skyblockID != "ASPECT_OF_THE_VOID") {
            AutoRouteUtils.setRotation(angles.first + offset, angles.second)
            Scheduler.schedulePreTickTask {
                ether()
            }
            return
        }
        ether()
    }


    override fun render(room: Room) {
        drawNode(room, etherwarpColor)
        if (!AutoRoute.drawEtherLines) return
        val nodeCoords = room.getRealCoords(pos)
        val targetCoords = room.getRealCoords(target)
        val lookVec = RotationUtils.getYawAndPitchOrigin(nodeCoords, targetCoords)
        if (edgeRoutes && lookVec.second.absoluteValue != 90f) {
            Renderer.draw3DLine(
                listOf(
                    nodeCoords.add(lookVec.first.xPart * 0.6, 0.0, lookVec.first.zPart * 0.6),
                    targetCoords,
                ),
                etherwarpColor,
                depth = depth
            )
        } else {
            Renderer.draw3DLine(
                listOf(
                    nodeCoords,
                    targetCoords
                ),
                etherwarpColor,
                depth = depth
            )
        }


    }

    override fun renderIndexColor(): Color {
        return etherwarpColor
    }

    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("target", target)
    }

    override fun loadNodeInfo(obj: JsonObject) {
        target = obj.get("target").asVec3
    }
}