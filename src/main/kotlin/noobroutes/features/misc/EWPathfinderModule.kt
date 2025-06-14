package noobroutes.features.misc

import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.AlwaysActive
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.pathfinding.Path
import noobroutes.utils.pathfinding.PathfinderExecutor
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer.drawBox
import noobroutes.utils.render.Renderer.drawStringInWorld
import noobroutes.utils.skyblock.devMessage

object EWPathfinderModule : Module(
    name = "Pathfinder",
    category = Category.MISC,
    description = "Etherwarp Pathfinder"
) {
    private val perfectPathing by BooleanSetting("Perfect Pathing", false, description = "Finds only the most optimal path.")
    private val yawStep by NumberSetting("Yaw Step", 1.5f, description = "Yaw step when raytracing.", min = 0.1f, max = 5f, increment = 0.1f, unit = "f")
    private val pitchStep by NumberSetting("Pitch Step", 1.5f, description = "Pitch step when raytracing.", min = 0.1f, max = 5f, increment = 0.1f, unit = "f")
    private val ewCost by NumberSetting("Etherwarp Cost", 20f, description = "Etherwarp Pathfinding cost.", min = 2f, max = 100f, increment = 2f, unit = "f")
    private val displayRaytrace by BooleanSetting("Raytrace Display", false, description = "Shows etherwarp blocks in the player's view")

    var lastPath: Path? = null
    var blocks: List<BlockPos>? = null
    var currentBlock: BlockPos? = null
    var bestHeuristic: Double = 0.0;

    fun execute(x: Float, y: Float, z: Float) {
        PathfinderExecutor.run(x, y, z, perfectPathing, yawStep, pitchStep, ewCost);
    }

    override fun onKeybind() {

    }

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent?) {

        devMessage("Best Heuristic : $bestHeuristic")

        if (lastPath == null) return

        var last = lastPath?.endNode
        while (last != null) {
            val pos = last.pos
            last = last.parent
            val aabb = AxisAlignedBB(pos, pos.add(1, 1, 1))
            drawBox(aabb, Color.GREEN, 3, 1, 0, false, true)
        }

        if (currentBlock != null) {
            val aabb = AxisAlignedBB(currentBlock, currentBlock!!.add(1, 1, 1))
            drawBox(aabb, Color.RED, 3, 1, 0, false, true)
        }
    }


}