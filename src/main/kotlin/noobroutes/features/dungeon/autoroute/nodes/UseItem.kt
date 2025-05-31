package noobroutes.features.dungeon.autoroute.nodes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.dungeon.autoroute.AutoRoute.useItemColor
import noobroutes.features.dungeon.autoroute.AutoRouteUtils
import noobroutes.features.dungeon.autoroute.Node
import noobroutes.utils.RotationUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.render.Color
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealYaw
import noobroutes.utils.skyblock.dungeon.tiles.Room
import noobroutes.utils.skyblock.modMessage

class UseItem(
    pos: Vec3 = Vec3(0.0, 0.0, 0.0),
    var itemName: String = "",
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
    "UseItem",
    5,
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
        AutoRouteUtils.setRotation(room.getRealYaw(yaw), pitch)
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
        val state = SwapManager.swapFromName(itemName)
        stopWalk()
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }

        if (state != SwapManager.SwapState.ALREADY_HELD) {
            AutoRouteUtils.setRotation(room.getRealYaw(yaw), pitch)
            Scheduler.scheduleLowestPreTickTask(1) {
                PlayerUtils.airClick()
            }
            return
        }
        Scheduler.scheduleLowestPreTickTask {
            PlayerUtils.airClick()
        }

    }


    override fun render(room: Room) {
        drawNode(room, useItemColor)

    }

    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("yaw", yaw)
        obj.addProperty("pitch", pitch)
        obj.addProperty("itemName", itemName)
    }

    override fun loadNodeInfo(obj: JsonObject) {
        this.yaw = obj.get("yaw").asFloat
        this.pitch = obj.get("pitch").asFloat
        this.itemName = obj.get("itemName").asString
    }

    override fun renderIndexColor(): Color {
        return useItemColor
    }
}