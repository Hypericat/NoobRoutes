package noobroutes.features.routes.nodes.autoroutes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.features.routes.AutoRoute
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.features.routes.nodes.NodeType
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.render.Color
import noobroutes.utils.routes.RouteUtils
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.sendChatMessage

class PearlClip(
    pos: Vec3,
    var distance: Int,
    awaitSecrets: Int = 0,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
    reset: Boolean = false,
): NodeLoader, AutorouteNode(
    pos,
    awaitSecrets,
    delay,
    center,
    stop,
    chain,
    reset
) {
    override val priority: Int = 7

    override fun nodeAddInfo(obj: JsonObject) {
        TODO("Not yet implemented")
    }

    override fun loadNodeInfo(obj: JsonObject): AutorouteNode {
        TODO("Not yet implemented")
    }

    override fun updateTick() {
        RouteUtils.setRotation(null, 90f, isSilent())
    }

    override fun run() {
        if (distance > 70) return modMessage("Invalid Clip Distance")
        stopWalk()
        val state = SwapManager.swapFromName("ender pearl")
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        RouteUtils.clipDistance = distance
        if (LocationUtils.isSinglePlayer) {
            Scheduler.schedulePreTickTask(1) { sendChatMessage("/tp ~ ~-$distance ~") }
        }
        RouteUtils.pearlSoundRegistered = true
        RouteUtils.setRotation(null, 90f, isSilent())
        RouteUtils.rightClick()    }

    override fun getType(): NodeType {
        return NodeType.PEARL_CLIP
    }

    override fun getRenderColor(): Color {
        return AutoRoute.pearlClipColor
    }
}