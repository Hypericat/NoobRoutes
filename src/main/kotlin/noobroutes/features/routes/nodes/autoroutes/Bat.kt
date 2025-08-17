package noobroutes.features.routes.nodes.autoroutes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.routes.AutoRoute
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.features.routes.nodes.NodeType
import noobroutes.utils.RotationUtils.offset
import noobroutes.utils.SwapManager
import noobroutes.utils.render.Color
import noobroutes.utils.routes.RouteUtils
import noobroutes.utils.routes.SecretUtils
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealYaw
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeYaw
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage

class Bat(
    pos: Vec3,
    val yaw : Float,
    val pitch: Float,
    awaitSecrets: Int = 0,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
    reset: Boolean = false

) : AutorouteNode(
    pos,
    awaitSecrets,
    delay,
    center,
    stop,
    chain,
    reset
) {
    override val priority: Int = 4

    companion object : NodeLoader {
        override fun loadNodeInfo(obj: JsonObject): AutorouteNode {
            val yaw = obj.get("yaw").asFloat
            val pitch = obj.get("pitch").asFloat
            val general = getGeneralNodeArgsFromObj(obj)
            return Bat(
                general.pos,
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
            val yaw = room.getRelativeYaw(mc.thePlayer.rotationYaw)
            val pitch = mc.thePlayer.rotationPitch
            val generalNodeArgs = getGeneralNodeArgs(room, args)
            return Bat(
                generalNodeArgs.pos,
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

    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("yaw", yaw)
        obj.addProperty("pitch", pitch)
    }



    override fun updateTick() {
        val room = currentRoom ?: return
        PlayerUtils.unSneak()
        RouteUtils.setRotation(room.getRealYaw(yaw),pitch + offset, AutoRoute.silent)
    }

    override fun run() {
        val room = currentRoom ?: return
        val state = if (LocationUtils.isSinglePlayer) SwapManager.swapFromId(267) else SwapManager.swapFromSBId("HYPERION", "ASTRAEA", "VALKYRIE", "SCYLLA", "NECRON_BLADE")
        stopWalk()
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        if (state == SwapManager.SwapState.UNKNOWN) {
            return
        }
        RouteUtils.setRotation(room.getRealYaw(yaw), pitch, AutoRoute.silent)
        SecretUtils.batSpawnRegistered = true
    }

    override fun getRenderColor(): Color {
        return AutoRoute.batColor
    }

    override fun getType(): NodeType {
        return NodeType.BAT
    }
}