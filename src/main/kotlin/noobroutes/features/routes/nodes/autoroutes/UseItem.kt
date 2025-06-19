package noobroutes.features.routes.nodes.autoroutes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.routes.AutoRoute
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.features.routes.nodes.NodeType
import noobroutes.utils.RotationUtils
import noobroutes.utils.SwapManager
import noobroutes.utils.Utils.containsOneOf
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.render.Color
import noobroutes.utils.routes.RouteUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealYaw
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeYaw
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage

class UseItem(
    pos: Vec3,
    var itemName: String,
    var yaw: Float,
    var pitch: Float,
    awaitSecrets: Int = 0,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
    reset: Boolean = false,
) : NodeLoader, AutorouteNode(
    pos,
    awaitSecrets,
    delay,
    center,
    stop,
    chain,
    reset
) {
    override val priority: Int = 5


    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("itemName", itemName)
    }

    override fun loadNodeInfo(obj: JsonObject): AutorouteNode {
        val pos = obj.get("position").asVec3
        val yaw = obj.get("yaw").asFloat
        val pitch = obj.get("pitch").asFloat
        val awaitSecrets = obj.get("secrets")?.asInt ?: 0
        val delay = obj.get("delay")?.asLong ?: 0L
        val center = obj.has("center")
        val stop = obj.has("stop")
        val chain = obj.has("chain")
        val reset = obj.has("reset")
        val itemName = obj.get("itemName").asString
        return UseItem(pos, itemName, yaw, pitch, awaitSecrets, delay, center, stop, chain, reset)
    }

    override fun generateFromArgs(
        args: Array<out String>,
        room: UniqueRoom
    ): AutorouteNode? {
        if (args.size < 3) {
            modMessage("Need Item Name")
            return null
        }
        val generalNodeArgs = getGeneralNodeArgs(room, args)
        val name = args[2].toString()
        val yaw = room.getRelativeYaw(mc.thePlayer.rotationYaw)
        val pitch = mc.thePlayer.rotationPitch
        return UseItem(
            generalNodeArgs.pos,
            name,
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

    override fun updateTick() {
        val room = currentRoom ?: return

        PlayerUtils.unSneak()
        RouteUtils.setRotation(room.getRealYaw(yaw), pitch + RotationUtils.offset, isSilent())
    }

    override fun run() {
        val room = currentRoom ?: return

        val state = SwapManager.swapFromName(itemName)
        stopWalk()
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        PlayerUtils.sneak()

        RouteUtils.setRotation(room.getRealYaw(yaw), pitch, AutoRoute.silent)
        RouteUtils.unsneak()
    }


    override fun getRenderColor(): Color {
        return AutoRoute.useItemColor
    }

    override fun getType(): NodeType {
        NodeType.USE_ITEM
    }
}