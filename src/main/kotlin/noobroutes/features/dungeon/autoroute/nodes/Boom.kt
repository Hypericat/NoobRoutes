package noobroutes.features.dungeon.autoroute.nodes

import com.google.gson.JsonObject
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.dungeon.autoroute.AutoRoute.depth
import noobroutes.features.dungeon.autoroute.Node
import noobroutes.utils.*
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.JsonUtils.asBlockPos
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.dungeonScanning.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeonScanning.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage

class Boom(
    pos: Vec3 = Vec3(0.0, 0.0, 0.0),
    var target: BlockPos = BlockPos(0.0, 0.0, 0.0),
    awaitSecret: Int = 0,
    maybeSecret: Boolean = false,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
    reset: Boolean = false,
) : Node(
    "Boom",
    9,
    pos,
    awaitSecret,
    maybeSecret,
    delay,
    center,
    stop,
    chain,
    reset
) {

    override fun drawIndex(index: Int, room: UniqueRoom) {
        Renderer.drawStringInWorld(index.toString(), room.getRealCoords(pos).add(Vec3(0.0, 0.6, 0.0)), renderIndexColor(), depth = depth)
    }

    override fun tick(room: UniqueRoom) {
        val pos = room.getRealCoords(target)
        if (isAir(pos)) {
            return
        }
        val state = SwapManager.swapFromSBId("INFINITE_SUPERBOOM_TNT", "SUPERBOOM_TNT")
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (!AutoRoute.silent) {
            val angles = RotationUtils.getYawAndPitch(pos.toVec3().add(0.5, 0.5, 0.5))
            RotationUtils.setAngles(angles.first, angles.second)
        }
        when (state) {
            SwapManager.SwapState.SWAPPED -> {
                AutoRoute.delay = System.currentTimeMillis() + 200
                Scheduler.schedulePreTickTask {
                    if (!isAir(pos)) {
                        AuraManager.auraBlock(pos, true)
                    }
                }
            }
            SwapManager.SwapState.ALREADY_HELD -> {
                AutoRoute.delay = System.currentTimeMillis() + 150
                if (!isAir(pos)) {
                    AuraManager.auraBlock(pos, true)
                }
            }
            else -> return
        }

    }
    override fun render(room: UniqueRoom) {
        drawNode(room, AutoRoute.boomColor)

    }

    override fun renderIndexColor(): Color {
        return AutoRoute.boomColor
    }

    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("target", target)
    }

    override fun loadNodeInfo(obj: JsonObject) {
        target = obj.get("target").asBlockPos
    }

}