package noobroutes.features.dungeon.autoroute.nodes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autoroute.AutoRoute.edgeRoutes
import noobroutes.features.dungeon.autoroute.AutoRoute.ether
import noobroutes.features.dungeon.autoroute.AutoRoute.silent
import noobroutes.features.dungeon.autoroute.Node
import noobroutes.features.floor7.autop3.AutoP3.depth
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
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.Room
import noobroutes.utils.toVec3

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
    listOf("warp", "ether"),
    pos,
    awaitSecret,
    maybeSecret,
    delay,
    center,
    stop,
    chain
) {
    override fun run(event: MotionUpdateEvent.Pre, room: Room) {
        val angles = RotationUtils.getYawAndPitch(room.getRealCoords(target))
        if (stop) PlayerUtils.stopVelocity()
        if (center) center()
        stopWalk()
        PlayerUtils.sneak()
        event.yaw = angles.first
        event.pitch = angles.second
        if (!silent) Scheduler.schedulePreTickTask { setAngles(angles.first, angles.second) }

        SwapManager.swapFromSBId("ASPECT_OF_THE_VOID")
        ether()
    }

    override fun render(room: Room) {
        Renderer.drawCylinder(room.getRealCoords(pos.add(Vec3(0.0, 0.03, 0.0))), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.GREEN, depth = depth)
        if (edgeRoutes) {
            val targetCoords = room.getRealCoords(target)
            val yaw = RotationUtils.getYawAndPitch(targetCoords).first
            Renderer.draw3DLine(
                listOf(
                    room.getRealCoords(pos).add(yaw.xPart * 0.6, 0.0, yaw.zPart * 0.6),
                    targetCoords,
                ),
                Color.GREEN
            )
        } else {
            Renderer.draw3DLine(
                listOf(
                    room.getRealCoords(pos),
                    room.getRealCoords(target)
                ),
                Color.GREEN
            )
        }


    }

    override fun getAsJsonObject(): JsonObject {
        return JsonObject().apply {
            addProperty("name", "Etherwarp")
            addProperty("position", pos)
            addProperty("target", target)
            if (awaitSecrets > 0) addProperty("awaitSecret", awaitSecrets)
            if (maybeSecret) addProperty("maybeSecret", true)
            if (delay > 0) addProperty("delay", delay)
            if (center) addProperty("center", true)
            if (stop) addProperty("stop", true)
            if (chain) addProperty("chain", true)
        }
    }
}