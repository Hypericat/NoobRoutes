package noobroutes.features.dungeon.autoroute.nodes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.dungeon.autoroute.AutoRoute.aotvColor
import noobroutes.features.dungeon.autoroute.AutoRoute.edgeRoutes
import noobroutes.features.dungeon.autoroute.Node
import noobroutes.utils.RotationUtils
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart
import noobroutes.utils.add
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealYaw
import noobroutes.utils.skyblock.dungeon.tiles.Room
import noobroutes.utils.json.JsonUtils.addProperty

class Aotv(
    pos: Vec3,
    var target: Vec3 = Vec3(0.0,0.0,0.0),
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
    4,
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

    override fun tick(room: Room) {
        if (!AutoRoute.silent) RotationUtils.setAngles(room.getRealYaw(yaw), pitch)

    }


    override fun render(room: Room) {
        drawNode(room, aotvColor)
        if (edgeRoutes) {
            val targetCoords = room.getRealCoords(target)
            val yaw = RotationUtils.getYawAndPitchOrigin(room.getRealCoords(pos), targetCoords, true).first
            Renderer.draw3DLine(
                listOf(
                    room.getRealCoords(pos).add(yaw.xPart * 0.6, 0.0, yaw.zPart * 0.6),
                    targetCoords,
                ),
                aotvColor
            )
        } else {
            Renderer.draw3DLine(
                listOf(
                    room.getRealCoords(pos),
                    room.getRealCoords(target)
                ),
                aotvColor
            )
        }
    }

    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("target", target)
    }





}