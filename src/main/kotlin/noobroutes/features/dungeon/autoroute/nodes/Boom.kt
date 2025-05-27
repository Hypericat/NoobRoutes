package noobroutes.features.dungeon.autoroute.nodes

import com.google.gson.JsonObject
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.dungeon.autoroute.Node
import noobroutes.features.dungeon.autoroute.SecretUtils
import noobroutes.utils.AuraManager
import noobroutes.utils.RotationUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.add
import noobroutes.utils.isAir
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.JsonUtils.asBlockPos
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.Room
import noobroutes.utils.toVec3

class Boom(
    pos: Vec3 = Vec3(0.0, 0.0, 0.0),
    var target: BlockPos = BlockPos(0.0, 0.0, 0.0),
    awaitSecret: Int = 0,
    maybeSecret: Boolean = false,
    delay: Long = 0,
    center: Boolean = false,
    stop: Boolean = false,
    chain: Boolean = false,
) : Node(
    "Boom",
    9,
    pos,
    awaitSecret,
    maybeSecret,
    delay,
    center,
    stop,
    chain
) {

    override fun tick(room: Room) {
        super.tick(room)
        val pos = room.getRealCoords(target)
        if (isAir(pos)) return

        AutoRoute.lastBoom = System.currentTimeMillis()
        val state = SwapManager.swapFromSBId("INFINITE_SUPERBOOM_TNT", "SUPERBOOM_TNT")

        if (!AutoRoute.silent) {
            val angles = RotationUtils.getYawAndPitch(pos.toVec3().add(0.5, 0.5, 0.5))
            RotationUtils.setAngles(angles.first, angles.second)
        }
        when (state) {
            SwapManager.SwapState.SWAPPED -> {
                Scheduler.schedulePreTickTask {
                    if (!isAir(pos)) {
                        AuraManager.auraBlock(pos, true)
                        Scheduler.schedulePreTickTask { this.runStatus = RunStatus.Complete }
                    }
                }
            }
            SwapManager.SwapState.ALREADY_HELD -> {
                if (!isAir(pos)) {
                    AuraManager.auraBlock(pos, true)
                    Scheduler.schedulePreTickTask { this.runStatus = RunStatus.Complete }
                }
            }
            else -> return
        }

    }
    override fun render(room: Room) {
        drawNode(room, AutoRoute.boomColor)
        //val pos = room.getRealCoords(target)
        //if (!isAir(pos)) Renderer.drawBlock(pos, AutoRoute.boomColor, depth = AutoRoute.depth)
    }

    override fun nodeAddInfo(obj: JsonObject) {
        obj.addProperty("target", target)
    }

    override fun loadNodeInfo(obj: JsonObject) {
        target = obj.get("target").asBlockPos
    }

}