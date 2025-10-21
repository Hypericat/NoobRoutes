package noobroutes.features.dungeon.brush

import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.utils.*
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage
import java.util.*
import kotlin.concurrent.thread


typealias MinBlockPos = BlockPos
typealias MaxBlockPos = BlockPos
object BrushBuildTools {

    fun handleUndo() {
        if (lastReplacedBlocks.isEmpty()) return modMessage("Nothing to undo")
        modMessage("§l§aUndoing")
        val blocksToUndo = lastReplacedBlocks.pop()
        thread {
            val blockList = BrushModule.getBlockList(blocksToUndo.second)
            blocksToUndo.first.forEach { (pos, save) ->
            val relative = blocksToUndo.second?.getRelativeCoords(pos) ?: pos

            blockList.remove(relative);
            BrushModule.removeBlockFromChunk(pos)
            if (save) {
                blockList.add(relative)
                BrushModule.addBlockToChunk(pos)
            }

            if (!isBlockLoaded(pos)) return@forEach
                // broken btw
                modMessage("undo broken!")
        }}.start()
    }


    private var lastReplacedBlocks: Stack<Pair<MutableList<Pair<BlockPos, Boolean>>, UniqueRoom?>> = Stack<Pair<MutableList<Pair<BlockPos, Boolean>>, UniqueRoom?>>()

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Unload) {
        lastReplacedBlocks.clear()
    }

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        BrushModule.consumeBlocks { pos -> renderBlock(pos) };
    }

    private fun renderBlock(blockPos: BlockPos) {
        Renderer.drawBox(AxisAlignedBB(blockPos, blockPos.add(1, 1, 1)), Color.GREEN, fillAlpha = 0.15f);
    }

}