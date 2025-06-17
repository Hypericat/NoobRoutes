package noobroutes.features.dungeon.autoroute.nodes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.dungeon.autoroute.AutoRoute.depth
import noobroutes.features.dungeon.autoroute.AutoRoute.pearlClipColor
import noobroutes.features.dungeon.autoroute.AutoRoute.silent
import noobroutes.features.dungeon.autoroute.AutoRouteUtils
import noobroutes.features.dungeon.autoroute.AutoRouteUtils.clipDistance
import noobroutes.features.dungeon.autoroute.AutoRouteUtils.pearlSoundRegistered
import noobroutes.features.dungeon.autoroute.Node
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeonScanning.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeonScanning.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.sendChatMessage

class PearlClip(
    pos: Vec3 = Vec3(0.0, 0.0, 0.0),
    var distance: Int = 0,
    awaitSecret: Int = 0,
    maybeSecret: Boolean = false,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
    reset: Boolean = false,
) : Node(
    "PearlClip",
    7,
    pos,
    awaitSecret,
    maybeSecret,
    delay,
    center,
    stop,
    chain,
    reset
) {
    override fun awaitTick(room: UniqueRoom) {
        if (!silent) mc.thePlayer.rotationPitch = 90f
    }

    override fun awaitMotion(
        event: MotionUpdateEvent.Pre,
        room: UniqueRoom
    ) {
        AutoRouteUtils.setRotation(null, 90f)
    }


    override fun tick(room: UniqueRoom) {
        if (distance > 70) return modMessage("Invalid Clip Distance")
        stopWalk()
        val state = SwapManager.swapFromName("ender pearl")
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        clipDistance = distance
        if (LocationUtils.isSinglePlayer) {
            Scheduler.schedulePreTickTask(1) { sendChatMessage("/tp ~ ~-$distance ~") }
        }
        pearlSoundRegistered = true

        if (!silent) mc.thePlayer.rotationPitch = 90f
        //PlayerUtils.forceUnSneak()
        Scheduler.schedulePreTickTask {
            PlayerUtils.airClick()
        }
    }

    override fun motion(
        event: MotionUpdateEvent.Pre,
        room: UniqueRoom
    ) {
        AutoRouteUtils.setRotation(null, 90f)
    }

    override fun render(room: UniqueRoom) {
        drawNode(room, pearlClipColor)
        if (AutoRoute.drawPearlClipText) Renderer.drawStringInWorld("PearlClip: $distance", room.getRealCoords(pos).add(Vec3(0.0, 0.9, 0.0)), pearlClipColor, depth = depth)
    }

    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("distance", distance)
    }

    override fun loadNodeInfo(obj: JsonObject) {
        this.distance = obj.get("distance").asInt
    }

    override fun renderIndexColor(): Color {
        return pearlClipColor
    }
}