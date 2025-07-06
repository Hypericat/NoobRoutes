package noobroutes.features.dungeon.brush

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.features.dungeon.brush.BrushModule
import noobroutes.ui.hud.Render
import noobroutes.utils.IBlockStateUtils
import noobroutes.utils.getBlockStateAt
import noobroutes.utils.isBlock
import noobroutes.utils.isBlockLoaded
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.runOnMCThread
import noobroutes.utils.setBlock
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage
import java.util.Collections
import java.util.HashSet
import java.util.Stack
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min


typealias MinBlockPos = BlockPos
typealias MaxBlockPos = BlockPos
object BrushBuildTools {
    

    var leftBlockPos: BlockPos? = null
    var rightBlockPos: BlockPos? = null

    fun fill(blocks: HashSet<BlockPos>, state: IBlockState) {
        val room = DungeonUtils.currentRoom
        setLastReplacedBlocks(blocks, room)

        val blocksToRegister = Collections.synchronizedList(mutableListOf<Pair<IBlockState, BlockPos>>())
        val setBlocks = HashSet<Pair<BlockPos, IBlockState>>()

        val blockPositionsToRegister = runBlocking {
            blocks.map { pos ->
                async {
                    setBlocks.add(pos to state)
                    room?.getRelativeCoords(pos) ?: pos
                }
            }.awaitAll().toSet()
        }

        runOnMCThread {
            for (pair in setBlocks) {
                if (isBlockLoaded(pair.first)) setBlock(pair.first, pair.second)
            }
        }

        setupBlocksToRegister(blockPositionsToRegister, blocksToRegister, room, state)
        BrushModule.registerChunkBlocks(room, blocksToRegister)
    }

    fun filteredFill(blocks: HashSet<BlockPos>, filter: Block, state: IBlockState) {
        if (state == IBlockStateUtils.airIBlockState) return
        val room = DungeonUtils.currentRoom
        val filteredBlocks = blocks.filter { pos ->
            isBlock(pos, filter)
        }
        setLastReplacedBlocks(filteredBlocks.toSet(), room)
        val blockPositionsToRegister = hashSetOf<BlockPos>()

        runOnMCThread {
            filteredBlocks.forEach { pos ->
                if (isBlockLoaded(pos)) setBlock(pos, state)
                blockPositionsToRegister.add(room?.getRelativeCoords(pos) ?: pos)
            }
            val blocksToRegister = mutableListOf<Pair<IBlockState, BlockPos>>()
            setupBlocksToRegister(blockPositionsToRegister, blocksToRegister, room, state)
            BrushModule.registerChunkBlocks(room, blocksToRegister)
        }
    }

    private fun setupBlocksToRegister(blockPositionsToRegister: Set<BlockPos>, blocksToRegister: MutableList<Pair<IBlockState, BlockPos>>, room: UniqueRoom?, state: IBlockState){
        val blockList =  BrushModule.getBlockList(room)
        blockPositionsToRegister.forEach { pos ->
            blocksToRegister.add(state to pos)
            BrushModule.removeBlockFromChunk(pos)
            val blockToAdd = Pair(state , pos)
            blockList.removeAll { it.second == pos }
            blockList.add(blockToAdd)
        }
    }

    fun handleUndo() {
        if (lastReplacedBlocks.isEmpty()) return modMessage("Nothing to undo")
        modMessage("§l§aUndoing")
        val blocksToUndo = lastReplacedBlocks.pop()
        thread {
            val blockList = BrushModule.getBlockList(blocksToUndo.second)
            blocksToUndo.first.forEach { (state, pos, save) ->
            val relative = blocksToUndo.second?.getRelativeCoords(pos) ?: pos

            blockList.removeAll { it.second == relative }
            BrushModule.removeBlockFromChunk(pos)
            if (save) {
                blockList.add(state to relative)
                BrushModule.addBlockToChunk(pos, state)
            }

            if (!isBlockLoaded(pos)) return@forEach
            setBlock(pos, state)
        }}.start()
    }


    private var lastReplacedBlocks: Stack<Pair<MutableList<Triple<IBlockState, BlockPos, Boolean>>, UniqueRoom?>> =
        Stack<Pair<MutableList<Triple<IBlockState, BlockPos, Boolean>>, UniqueRoom?>>()

    fun setLastReplacedBlocks(blockPositionsToRegister: Set<BlockPos>, room: UniqueRoom?){
        val blocksToRegister = mutableListOf<Triple<IBlockState, BlockPos, Boolean>>()
        val blockList = BrushModule.getBlockList(room)
        blockPositionsToRegister.forEach { pos ->
            blocksToRegister.add(Triple(getBlockStateAt(pos), pos, blockList.any { it.second == pos }))
        }

        lastReplacedBlocks.push(Pair(blocksToRegister, room))
    }
    fun getSelectedArea(): HashSet<BlockPos> {
        val rightPos = rightBlockPos ?: return hashSetOf()
        val leftPos = leftBlockPos ?: return hashSetOf()
        val areaList = hashSetOf<BlockPos>()
        val bounds = getBoundsFromBlockPositions(rightPos, leftPos)
        val min = bounds.first
        val max = bounds.second

        for (x in min.x..max.x) {
            for (y in min.y..max.y) {
                for (z in min.z..max.z) {
                    areaList.add(BlockPos(x, y, z))
                }
            }
        }
        return areaList
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Unload) {
        lastReplacedBlocks.clear()
    }

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        if (!BrushModule.editMode) return
        val rightPos = rightBlockPos
        val leftPos = leftBlockPos
        //the reason it is structured like this is so that the selected blocks render on top of the box
        if (leftPos != null && rightPos != null) {
            val bounds = getBoundsFromBlockPositions(rightPos, leftPos)
            Renderer.drawBox(AxisAlignedBB(bounds.first, bounds.second.add(1, 1, 1)), Color.GREEN, fillAlpha = 0.15f)
        }
        rightPos?.let {
            Renderer.drawBlock(it, Color(254, 138, 2), fillAlpha = 0f)
        }
        leftPos?.let {
            Renderer.drawBlock(it, Color(6, 141, 255), fillAlpha = 0f)
        }
    }

    fun getBoundsFromBlockPositions(firstPos: BlockPos, secondPos: BlockPos): Pair<MinBlockPos, MaxBlockPos> {
        val minX = min(firstPos.x, secondPos.x)
        val minY = min(firstPos.y, secondPos.y)
        val minZ = min(firstPos.z, secondPos.z)
        val maxX = max(firstPos.x, secondPos.x)
        val maxY = max(firstPos.y, secondPos.y)
        val maxZ = max(firstPos.z, secondPos.z)
        return BlockPos(minX, minY, minZ) to MinBlockPos(maxX, maxY, maxZ)
    }

}