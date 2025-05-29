package noobroutes.features.dungeon.autoroute.nodes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.dungeon.autoroute.AutoRoute.useItemColor
import noobroutes.features.dungeon.autoroute.Node
import noobroutes.utils.*
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
    var sneak: Boolean = false,
    awaitSecret: Int = 0,
    maybeSecret: Boolean = false,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
    reset: Boolean = false,
) : Node(
    "Aotv",
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
        val state = SwapManager.swapFromSBId(itemName)
        stopWalk()
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }

        if (mc.thePlayer.isSneaking != sneak || state != SwapManager.SwapState.ALREADY_HELD) {
            PlayerUtils.setSneak(sneak)
            AutoRoute.rotatingPitch = pitch
            AutoRoute.rotatingYaw = room.getRealYaw(yaw)
            AutoRoute.rotating = true
            Scheduler.scheduleLowestPreTickTask {
                PlayerUtils.airClick()
            }
            return
        }
        PlayerUtils.airClick()
    }


    override fun render(room: Room) {
        drawNode(room, useItemColor)

    }

    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("yaw", yaw)
        obj.addProperty("pitch", pitch)
        obj.addProperty("sneak", sneak)
        obj.addProperty("itemName", itemName)
    }

    override fun loadNodeInfo(obj: JsonObject) {
        this.yaw = obj.get("yaw").asFloat
        this.pitch = obj.get("pitch").asFloat
        this.itemName = obj.get("itemName").asString
        this.sneak = obj.get("sneak").asBoolean
    }

    override fun renderIndexColor(): Color {
        return useItemColor
    }
}