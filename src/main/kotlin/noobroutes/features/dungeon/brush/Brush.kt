package noobroutes.features.dungeon.brush

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.ResourceLocation
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.Core
import noobroutes.config.DataManager
import noobroutes.events.impl.ClickEvent
import noobroutes.events.impl.LocationChangeEvent
import noobroutes.events.impl.RoomEnterEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.render.FreeCam
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.IBlockStateUtils
import noobroutes.utils.getBlockStateAt
import noobroutes.utils.json.JsonUtils.add
import noobroutes.utils.json.JsonUtils.asBlockPos
import noobroutes.utils.runOnMCThread
import noobroutes.utils.setBlock
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import org.lwjgl.input.Keyboard
import kotlin.collections.iterator

object Brush : Module("Brush", description = "It is just fme but way less laggy.", category = Category.DUNGEON) {
    const val BIT_MASK = 0xFFF0

    var editMode by BooleanSetting("Edit Mode", description = "Allows you to edit blocks")
    val editModeToggle by KeybindSetting("Edit Mode Bind", Keyboard.KEY_NONE, description = "Toggles Edit Mode").onPress {
        editMode = !editMode
        if (!editMode) {
            saveConfig()
        }
    }
    val placeCooldown by NumberSetting(
        "Place Cooldown",
        min = 0,
        max = 1000,
        default = 150,
        description = "Cooldown between placing blocks in edit mode",
        unit = "ms"
    )

    var savedChunks = hashMapOf<Pair<Int, Int>, HashMap<BlockPos, IBlockState>>()
    var blockConfig: MutableMap<String, MutableList<Pair<IBlockState, BlockPos>>> = mutableMapOf()

    var selectedBlockState: IBlockState = IBlockStateUtils.airIBlockState
    private var lastPlace = System.currentTimeMillis()


    private fun calculateChunkHash(chunk: Chunk) : Pair<Int, Int> {
        return Pair(chunk.xPosition, chunk.zPosition)
    }


    private fun calculateChunkHash(x: Int, z: Int) : Pair<Int, Int> {
        return Pair(x, z)
    }

    fun onChunkLoad(chunk: Chunk) {
        if (!enabled) return
        val blocks = savedChunks.get(calculateChunkHash(chunk));
        if (blocks == null) return

        blocks.forEach { (pos, state) ->
            setBlock(pos, state)
        }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        savedChunks.clear()
        if (editMode) {
            editMode = false
            saveConfig()
        }
    }



    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onMouse(event: ClickEvent.All) {
        if (!editMode) return
        val mouseOver = if (FreeCam.enabled) FreeCam.looking else mc.objectMouseOver ?: return
        val target = mouseOver?.hitBlock() ?: return
        val room = DungeonUtils.currentRoom
        event.isCanceled = true

        if (event.type == ClickEvent.ClickType.Middle) {
            selectedBlockState = getBlockStateAt(target)
        }

        val blockList = blockConfig.getOrPut(room?.name ?: getLocation()) { mutableListOf() }
        if (event.type == ClickEvent.ClickType.Left) {
            val pos = room?.getRelativeCoords(target) ?: target

            val removed = blockList.removeAll{ (_, blockPos) ->
                pos == blockPos;
            }

            if (removed) {
                runOnMCThread {
                    val hash = Pair(pos.x shr 4, pos.z shr 4)
                    savedChunks.getOrElse(hash) { null }?.remove(pos) ?: return@runOnMCThread
                    setBlock(pos, IBlockStateUtils.airIBlockState);
                }
                return
            }

            blockList.add(Pair(IBlockStateUtils.airIBlockState, pos))
            runOnMCThread {
                val hash = Pair(pos.x shr 4, pos.z shr 4)
                savedChunks.getOrPut(hash) { hashMapOf() }.put(pos, IBlockStateUtils.airIBlockState)
                setBlock(pos, IBlockStateUtils.airIBlockState);
            }
            return;
        }

        if (event.type == ClickEvent.ClickType.Right) {
            if (System.currentTimeMillis() - lastPlace < placeCooldown || selectedBlockState == IBlockStateUtils.airIBlockState) return
            lastPlace = System.currentTimeMillis()
            val facing = mouseOver.sideHit
            val offset = target.offset(facing)
            val pos = room?.getRealCoords(offset) ?: offset

            blockList.add(Pair(selectedBlockState, pos))
            runOnMCThread {
                val hash = Pair(pos.x shr 4, pos.z shr 4)
                savedChunks.getOrPut(hash) { hashMapOf() }.put(pos, selectedBlockState)
                setBlock(pos, selectedBlockState);
            }
        }
    }

    @SubscribeEvent
    fun locationEvent(event: LocationChangeEvent){
        val location = if (event.location.displayName == "Catacombs")
            "Floor ${DungeonUtils.floorNumber}" else event.location.displayName

        registerChunkBlocks(null, blockConfig[location] ?: return)
    }

    @SubscribeEvent
    fun onRoomEnterEvent(event: RoomEnterEvent){
        if (event.room == null) return
        val list = blockConfig[event.room.name] ?: return

        registerChunkBlocks(event.room, list)
    }





    private fun registerChunkBlocks(room: UniqueRoom?, positions: List<Pair<IBlockState, BlockPos>>) {
        runOnMCThread {
            positions.forEach { (state, localPos) ->
                val pos = room?.getRealCoords(localPos) ?: localPos

                //  Optimization over normal calculateChunkHash, the x is calculated the same way.
                //  For the z, instead of shifting right by 4 and shifting left by 16
                //  we and it with the bit mask (zeroes the bottom 4 bits) and shift it by only 12
                val hash = calculateChunkHash(pos.x shr 4, pos.z shr 4)

                savedChunks.getOrPut(hash) { hashMapOf() }.put(pos, state)
            }
        }
    }


    private fun addChunkBlock(room: UniqueRoom?, pair: Pair<IBlockState, BlockPos>) {
        runOnMCThread {
            val pos = room?.getRealCoords(pair.second) ?: pair.second

            val hash = Pair(pos.x shr 4, pos.z shr 4)
            savedChunks.getOrPut(hash) { hashMapOf() }.put(pos, pair.first)
            setBlock(pos, pair.first);
        }
    }

    private fun removeChunkBlock(room: UniqueRoom?, target: BlockPos) {
        runOnMCThread {
            val pos = room?.getRealCoords(target) ?: target

            val hash = Pair(pos.x shr 4, pos.z shr 4)

            savedChunks.getOrElse(hash) { null }?.remove(pos) ?: return@runOnMCThread
            setBlock(pos, IBlockStateUtils.airIBlockState);
        }
    }

    fun getEditedBlock(blockPos: BlockPos): IBlockState? {
        val hash = calculateChunkHash(blockPos.x shr 4, blockPos.z shr 4)
        val blocks = savedChunks[hash] ?: return null
        return blocks[blockPos]
    }


    private fun MovingObjectPosition.hitBlock(): BlockPos? {
        return if (this.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) this.blockPos else null
    }

    private fun getLocation(): String {
        val location = LocationUtils.currentArea
        if (location.displayName == "Catacombs") {
            return "Floor ${DungeonUtils.floorNumber}"
        }
        return location.displayName
    }



    fun loadConfig() {
        val file = DataManager.loadDataFromFileObjectOfObjects("editedblocks")
        blockConfig.clear()
        savedChunks.clear()
        for ((area, blocks) in file) {
            Core.logger.info(area)
            val blockList = mutableListOf<Pair<IBlockState, BlockPos>>()
            val areaObject = blocks.asJsonObject

            for ((key, jsonArray) in areaObject.entrySet()) {
                val (blockId, metaString) = key.split(";")
                Core.logger.info("$blockId;$metaString")
                val meta = metaString.toIntOrNull() ?: continue
                val block = Block.blockRegistry.getObject(ResourceLocation(blockId)) ?: continue
                val state = block.getStateFromMeta(meta)

                for (posElement in jsonArray.asJsonArray) {
                    blockList.add(state to posElement.asBlockPos)
                }
            }
            blockConfig[area] = blockList
        }
    }

    fun saveConfig() {
        val root = JsonObject()

        for ((area, blocks) in blockConfig) {
            val areaObject = JsonObject()

            val blockSudoMap = mutableMapOf<String, MutableList<BlockPos>>()
            for ((state, pos) in blocks) {
                val block = state.block
                val registryName = Block.blockRegistry.getNameForObject(block)
                val meta = block.getMetaFromState(state)
                val key = "$registryName;$meta"
                blockSudoMap.getOrPut(key) { ArrayList() }.add(pos)
            }
            blockSudoMap.forEach { meta, list ->
                val array = JsonArray()
                list.forEach {
                    array.add(it)
                }
                areaObject.add(meta, array)
            }
            root.add(area, areaObject)
        }
        DataManager.saveDataToFile("editedblocks", root)
    }


    }