package noobroutes.features.misc

import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.Core
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.dungeon.autoroute.DynNode
import noobroutes.features.dungeon.autoroute.nodes.Etherwarp
import noobroutes.features.move.DynamicRoute
import noobroutes.features.render.ClickGUIModule
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.pathfinding.Path
import noobroutes.pathfinding.PathFinder
import noobroutes.pathfinding.PathNode
import noobroutes.pathfinding.PathfinderExecutor
import noobroutes.utils.*
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer.drawBox
import noobroutes.utils.skyblock.EtherWarpHelper
import noobroutes.utils.skyblock.EtherWarpHelper.EYE_HEIGHT
import noobroutes.utils.skyblock.EtherWarpHelper.EtherPos
import noobroutes.utils.skyblock.EtherWarpHelper.isValidEtherWarpBlock
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import java.util.*
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sign

object EWPathfinderModule : Module(
    name = "Pathfinder",
    category = Category.MISC,
    description = "Etherwarp Pathfinder"
) {
    private val perfectPathing by BooleanSetting("Perfect Pathing", false, description = "Finds only the most optimal path.")
    private val yawStep by NumberSetting("Yaw Step", 1f, description = "Yaw step when raytracing.", min = 0.1f, max = 5f, increment = 0.1f, unit = "f")
    private val pitchStep by NumberSetting("Pitch Step", 1f, description = "Pitch step when raytracing.", min = 0.1f, max = 5f, increment = 0.1f, unit = "f")
    private val ewCost by NumberSetting("Etherwarp Cost", 100f, description = "Etherwarp Pathfinding cost.", min = 10f, max = 200f, increment = 5f, unit = "f")
    private val heuristicThreshold by NumberSetting("Heuristic Threshold", 2f, description = "Use greater values for more complex rooms, default 2.", min = 0.5f, max = 10f, increment = 0.5f, unit = "f")

    private val displayDebug by BooleanSetting("Debug Display", false, description = "Shows pathfinder debug positions").withDependency { ClickGUIModule.devMode }

    var lastPath: Path? = null
    var blocks: List<BlockPos>? = null
    var currentBlock: BlockPos? = null
    var bestHeuristic: Double = 0.0

    fun execute(x: Float, y: Float, z: Float) {
        PathfinderExecutor.run(x, y, z, perfectPathing, yawStep, pitchStep, ewCost, heuristicThreshold)
    }

    fun onSolve(path : Path) {
        this.lastPath = path

        val currentRoom = DungeonUtils.currentRoom ?: AutoRoute.roomReplacement

        var lastNode: PathNode? = null
        var node: PathNode? = path.endNode

        while (node != null) {
            if (lastNode != null) {
                val nodeVec3 = Vec3(node.pos.x.toDouble() + 0.5, node.pos.y.toDouble() + 1, node.pos.z + 0.5)
                val targetVec3 : Vec3? = getEtherPosFromOrigin(nodeVec3.add(0.0, EYE_HEIGHT, 0.0), lastNode.yaw, lastNode.pitch)

                if (targetVec3 == null) {
                    System.err.println("Invalid YAW / PITCH : " + lastNode.yaw + " : " + lastNode.pitch)
                    lastNode = node
                    node = node.parent
                    continue
                }

                DynamicRoute.addNode(DynNode(nodeVec3, targetVec3))
            }
            lastNode = node
            node = node.parent
        }
    }

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent?) {
        if (!displayDebug || !ClickGUIModule.devMode) return

        if (lastPath != null) {
            var last = lastPath?.endNode
            while (last != null) {
                val pos = last.pos
                last = last.parent
                val aabb = AxisAlignedBB(pos, pos.add(1, 1, 1))
                drawBox(aabb, Color.GREEN, 3, 1, 0, false, true)
            }
        }

        if (currentBlock != null) {
            val aabb = AxisAlignedBB(currentBlock, currentBlock!!.add(1, 1, 1))
            drawBox(aabb, Color.RED, 3, 1, 0, false, true)
        }
    }


    /**
     * Traverses voxels from start to end and returns the first non-air block it hits.
     * @author Bloom
     */
    fun traverseVoxels(start: Vec3, end: Vec3): Vec3? {
        val direction = end.subtract(start)
        val step = IntArray(3) { sign(direction[it]).toInt() }
        val invDirection = DoubleArray(3) { if (direction[it] != 0.0) 1.0 / direction[it] else Double.MAX_VALUE }
        val tDelta = DoubleArray(3) { abs(invDirection[it]) }
        val currentPos = IntArray(3) { floor(start[it]).toInt() }
        val endPos = IntArray(3) { floor(end[it]).toInt() }

        val tMax = DoubleArray(3) {
            val startCoord = start[it]
            val offset = if (step[it] > 0) (floor(startCoord) + 1 - startCoord) else (startCoord - floor(startCoord))
            offset * abs(invDirection[it])
        }

        var t = 0.0

        repeat(1000) {
            val blockPos = BlockPos(currentPos[0], currentPos[1], currentPos[2])
            if (getBlockIdAt(blockPos) != 0) {
                // Calculate hit point
                val hitX = start.xCoord + direction.xCoord * t
                val hitY = start.yCoord + direction.yCoord * t
                val hitZ = start.zCoord + direction.zCoord * t
                return Vec3(hitX, hitY, hitZ)
            }
            if (currentPos.contentEquals(endPos)) return null

            // Determine which axis to step
            val axis = if (tMax[0] <= tMax[1])
                if (tMax[0] <= tMax[2]) 0 else 2
            else
                if (tMax[1] <= tMax[2]) 1 else 2

            currentPos[axis] += step[axis]
            t = tMax[axis]
            tMax[axis] += tDelta[axis]
        }

        return null
    }

    /**
     * Gets the position of an entity in the "ether" based on the origin's view direction.
     *
     * @param origin The initial position of the entity.
     * @param yaw The yaw angle representing the player's horizontal viewing direction.
     * @param pitch The pitch angle representing the player's vertical viewing direction.
     * @return An `EtherPos` representing the calculated position in the "ether" or `EtherPos.NONE` if the player is not present.
     */
    fun getEtherPosFromOrigin(origin: Vec3, yaw: Float, pitch: Float, distance: Double = 61.0, returnEnd: Boolean = false): Vec3? {
        Core.mc.thePlayer ?: return null;
        val endPos = getLook(yaw = yaw, pitch = pitch).normalize().multiply(factor = distance).add(origin)
        return traverseVoxels(origin, endPos);
    }


}