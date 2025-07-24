package noobroutes.features.routes.nodes.autoroutes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.features.routes.AutoRoute
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.features.routes.nodes.NodeType
import noobroutes.utils.RotationUtils
import noobroutes.utils.RotationUtils.offset
import noobroutes.utils.SwapManager
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart
import noobroutes.utils.add
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.routes.RouteUtils
import noobroutes.utils.routes.RouteUtils.swapToEtherwarp
import noobroutes.utils.skyblock.EtherWarpHelper
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage
import kotlin.math.absoluteValue

class Etherwarp(
    pos: Vec3,
    val target: Vec3,
    awaitSecrets: Int = 0,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
    reset: Boolean = false,
) : AutorouteNode(
    pos,
    awaitSecrets,
    delay,
    center,
    stop,
    chain,
    reset
) {

    companion object : NodeLoader{
        override fun loadNodeInfo(obj: JsonObject): AutorouteNode {
            val general =  getGeneralNodeArgsFromObj(obj)
            val target = obj.get("target").asVec3
            return Etherwarp(
                general.pos,
                target,
                general.awaitSecrets,
                general.delay,
                general.center,
                general.stop,
                general.chain,
                general.reset
            )
        }

        override fun generateFromArgs(args: Array<out String>, room: UniqueRoom): AutorouteNode? {
            val generalNodeArgs = getGeneralNodeArgs(room, args)
            val raytrace = EtherWarpHelper.rayTraceBlock(200, 1f)
            if (raytrace == null) {
                modMessage("No Target Found")
                return null
            }
            val target = room.getRelativeCoords(raytrace)
            return Etherwarp(
                generalNodeArgs.pos,
                target,
                generalNodeArgs.awaitSecrets,
                generalNodeArgs.delay,
                generalNodeArgs.center,
                generalNodeArgs.stop,
                generalNodeArgs.chain,
                generalNodeArgs.reset
            )
        }
    }

    override val priority: Int = 8

    override fun render() {
        super.render()
        if (!AutoRoute.drawEtherLines) return
        val room = currentRoom ?: return
        val nodeCoords = room.getRealCoords(pos)
        val targetCoords = room.getRealCoords(target)
        val lookVec = RotationUtils.getYawAndPitchOrigin(nodeCoords, targetCoords, true)
        if (AutoRoute.edgeRoutes && lookVec.second.absoluteValue != 90f) {
            Renderer.draw3DLine(
                listOf(
                    nodeCoords.add(lookVec.first.xPart * 0.6, 0.0, lookVec.first.zPart * 0.6),
                    targetCoords,
                ),
                getRenderColor(),
                depth = getDepth()
            )
        } else {
            Renderer.draw3DLine(
                listOf(
                    nodeCoords,
                    targetCoords
                ),
                getRenderColor(),
                depth = getDepth()
            )
        }
    }

    override fun getRenderColor(): Color {
        return AutoRoute.etherwarpColor
    }


    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("target", target)
    }



    override fun run() {
        val room = currentRoom ?: return
        val angles = RotationUtils.getYawAndPitch(room.getRealCoords(target), true)
        swapToEtherwarp()
        RouteUtils.setRotation(angles.first,angles.second, isSilent())
        stopWalk()
        PlayerUtils.unSneak()
        RouteUtils.ether()
    }

    override fun updateTick() {
        val room = currentRoom ?: return
        val angles = RotationUtils.getYawAndPitch(room.getRealCoords(target), true)
        RouteUtils.setRotation(angles.first + offset,angles.second, isSilent())
    }

    override fun getType(): NodeType {
        return NodeType.ETHERWARP
    }

}