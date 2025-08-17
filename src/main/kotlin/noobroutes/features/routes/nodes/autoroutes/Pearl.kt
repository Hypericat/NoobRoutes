package noobroutes.features.routes.nodes.autoroutes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.routes.AutoRoute
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.features.routes.nodes.NodeType
import noobroutes.utils.RotationUtils
import noobroutes.utils.SwapManager
import noobroutes.utils.render.Color
import noobroutes.utils.routes.RouteUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealYaw
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeYaw
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage

class Pearl(
    pos: Vec3,
    var count: Int = 0,
    val yaw : Float,
    val pitch: Float,
    awaitSecrets: Int = 0,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
    reset: Boolean = false
) :  AutorouteNode(
    pos,
    awaitSecrets,
    delay,
    center,
    stop,
    chain,
    reset
) {

    companion object : NodeLoader {
        override fun loadNodeInfo(obj: JsonObject): AutorouteNode {
            val count = obj.get("count").asInt
            val yaw = obj.get("yaw").asFloat
            val pitch = obj.get("pitch").asFloat
            val general = getGeneralNodeArgsFromObj(obj)

            return Pearl(
                general.pos,
                count,
                yaw,
                pitch,
                general.awaitSecrets,
                general.delay,
                general.center,
                general.stop,
                general.chain,
                general.reset
            )
        }

        override fun generateFromArgs(
            args: Array<out String>,
            room: UniqueRoom
        ): AutorouteNode? {
            if (args.size < 3) {
                modMessage("Pearl Count")
                return null
            }
            val generalNodeArgs = getGeneralNodeArgs(room, args)
            val count = args[2].toIntOrNull()
            if (count == null) {
                modMessage("Input a number")
                return null
            }
            val yaw = room.getRelativeYaw(mc.thePlayer.rotationYaw)
            val pitch = mc.thePlayer.rotationPitch
            return Pearl(
                generalNodeArgs.pos,
                count,
                yaw,
                pitch,
                generalNodeArgs.awaitSecrets,
                generalNodeArgs.delay,
                generalNodeArgs.center,
                generalNodeArgs.stop,
                generalNodeArgs.chain,
                generalNodeArgs.reset
            )
        }
    }

    override val priority: Int = 3


    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("yaw", yaw)
        obj.addProperty("pitch", pitch)
        obj.addProperty("count", count)
    }



    override fun updateTick() {
        val room = currentRoom ?: return
        RouteUtils.setRotation(room.getRealYaw(yaw),pitch + RotationUtils.offset, isSilent())
        PlayerUtils.unSneak(true)
    }

    override fun run() {
        val room = currentRoom ?: return
        if (count < 1) return modMessage("Invalid Pearl Count")
        val state = SwapManager.swapFromName("ender pearl")
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        PlayerUtils.sneak()
        RouteUtils.setRotation(room.getRealYaw(yaw), pitch, isSilent())
        RouteUtils.unsneak()
        RouteUtils.pearls = count - 1
    }

    override fun getType(): NodeType {
        return NodeType.PEARL
    }

    override fun getRenderColor(): Color {
        return AutoRoute.pearlColor
    }


}