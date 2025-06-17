package noobroutes.features.dungeon.autoroute.nodes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.dungeon.autoroute.AutoRoute.depth
import noobroutes.features.dungeon.autoroute.AutoRoute.pearlColor
import noobroutes.features.dungeon.autoroute.AutoRoute.silent
import noobroutes.features.dungeon.autoroute.AutoRouteUtils
import noobroutes.features.dungeon.autoroute.Node
import noobroutes.utils.RotationUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealYaw
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage

class Pearl(
    pos: Vec3 = Vec3(0.0, 0.0, 0.0),
    var count: Int = 0,
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
    "Pearl",
    3,
    pos,
    awaitSecret,
    maybeSecret,
    delay,
    center,
    stop,
    chain,
    reset
) {
    private var pearlsThrown = 0

    override fun reset() {
        delayTriggered = false
        secretTriggered = false
        centerTriggered = false
        resetTriggered = false
        delete = true
        pearlsThrown = 0
        triggered = false
    }

    override fun awaitTick(room: UniqueRoom) {
        if (!silent) RotationUtils.setAngles(room.getRealYaw(yaw), pitch)
        PlayerUtils.unSneak()
    }

    override fun awaitMotion(
        event: MotionUpdateEvent.Pre,
        room: UniqueRoom
    ) {
        AutoRouteUtils.setRotation(room.getRealYaw(yaw), pitch)
    }


    override fun tick(room: UniqueRoom) {
        if (count < 1) return modMessage("Invalid Clip Distance")
        PlayerUtils.unSneak()
        delete = false
        val state = SwapManager.swapFromName("ender pearl")
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        pearlsThrown++

        if (!silent) RotationUtils.setAngles(room.getRealYaw(yaw), pitch)
        Scheduler.schedulePreTickTask {
            PlayerUtils.airClick()
        }

        if (pearlsThrown >= count) delete = true
    }

    override fun motion(
        event: MotionUpdateEvent.Pre,
        room: UniqueRoom
    ) {
        //event.pitch = pitch
        //event.yaw = room.getRealYaw(yaw)
        AutoRouteUtils.setRotation(pitch, room.getRealYaw(yaw))
    }

    override fun render(room: UniqueRoom) {
        drawNode(room, pearlColor)
        if (AutoRoute.drawPearlCountText) Renderer.drawStringInWorld("Pearls: $count", room.getRealCoords(pos).add(Vec3(0.0, 0.9, 0.0)), pearlColor, depth = depth)
    }

    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("count", count)
        obj.addProperty("yaw", yaw)
        obj.addProperty("pitch", pitch)
    }

    override fun loadNodeInfo(obj: JsonObject) {
        this.count = obj.get("count").asInt
        this.yaw = obj.get("yaw").asFloat
        this.pitch = obj.get("pitch").asFloat
    }

    override fun renderIndexColor(): Color {
        return pearlColor
    }
}