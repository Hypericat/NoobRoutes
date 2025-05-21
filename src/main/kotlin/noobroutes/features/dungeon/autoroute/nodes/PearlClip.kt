package noobroutes.features.dungeon.autoroute.nodes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.dungeon.autoroute.AutoRoute.depth
import noobroutes.features.dungeon.autoroute.AutoRoute.silent
import noobroutes.features.dungeon.autoroute.Node
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.Room
import noobroutes.utils.skyblock.modMessage

class PearlClip(
    pos: Vec3,
    var distance: Int,
    awaitSecret: Int = 0,
    maybeSecret: Boolean = false,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
) : Node(
    "PearlClip",
    3,
    pos,
    awaitSecret,
    maybeSecret,
    delay,
    center,
    stop,
    chain
) {
    override fun awaitTick(room: Room) {
        if (!silent) mc.thePlayer.rotationPitch = 90f
    }

    override fun awaitMotion(
        event: MotionUpdateEvent.Pre,
        room: Room
    ) {
        AutoRoute.rotatingYaw = null
        AutoRoute.rotatingPitch = 90f
        AutoRoute.rotating = true
    }


    override fun tick(room: Room) {
        if (distance > 70) return modMessage("Invalid Clip Distance")
        SwapManager.swapFromName("ender pearl")
        AutoRoute.pearlSoundRegistered = true
        AutoRoute.clipDistance = distance
        if (!silent) mc.thePlayer.rotationPitch = 90f
        PlayerUtils.airClick()
    }

    override fun motion(
        event: MotionUpdateEvent.Pre,
        room: Room
    ) {
        event.pitch = 90f
    }

    override fun render(room: Room) {
        drawNode(room, AutoRoute.pearlClipColor)
        Renderer.drawStringInWorld("PearlClip: $distance", pos.add(Vec3(0.0, 0.6, 0.0)), AutoRoute.pearlClipColor, depth = depth)
    }

    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("distance", distance)
    }
}