package noobroutes.features.dungeon.brush

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.block.Block
import net.minecraft.block.properties.IProperty
import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
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
import noobroutes.mixin.accessors.ChunkListingFieldAccessor
import noobroutes.utils.IBlockStateUtils
import noobroutes.utils.IBlockStateUtils.withProperty
import noobroutes.utils.Utils.ID
import noobroutes.utils.add
import noobroutes.utils.capitalizeFirst
import noobroutes.utils.getBlockStateAt
import noobroutes.utils.json.JsonUtils.add
import noobroutes.utils.json.JsonUtils.asBlockPos
import noobroutes.utils.render.RenderUtils.renderVec
import noobroutes.utils.runOnMCThread
import noobroutes.utils.setBlock
import noobroutes.utils.skyblock.EtherWarpHelper
import noobroutes.utils.skyblock.Island
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.toBlockPos
import noobroutes.utils.toVec3
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.util.ArrayList
import java.util.HashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.iterator

@Suppress("unused")
object BrushModule : Module("Brush", description = "It is just fme but way less laggy. Works with FME floor config, but not the room config.", category = Category.DUNGEON) {

    private val hotbarPalette by BooleanSetting(
        "Hot Bar Palette",
        description = "Gets the selected block from the held item"
    )
    private val placeCooldown by NumberSetting(
        "Place Cooldown",
        min = 0,
        max = 1000,
        default = 150,
        description = "Cooldown between placing blocks in edit mode",
        unit = "ms"
    )
    var editMode by BooleanSetting("Edit Mode", description = "Allows you to edit blocks")
    private val editModeToggle by KeybindSetting(
        "Edit Mode Bind",
        Keyboard.KEY_NONE,
        description = "Toggles Edit Mode"
    ).onPress {
        toggleEditMode()
    }

    private var savedChunks = hashMapOf<Long, HashMap<BlockPos, IBlockState>>()

    private val floorConfig = ConcurrentHashMap<String, MutableList<Pair<IBlockState, BlockPos>>>()
    private val roomConfig = ConcurrentHashMap<String, MutableList<Pair<IBlockState, BlockPos>>>()

    var selectedBlockState: IBlockState = IBlockStateUtils.airIBlockState
    private var lastPlace = System.currentTimeMillis()



    private fun calculateChunkHash(chunk: Chunk): Long {
        return calculateChunkHash(chunk.xPosition, chunk.zPosition)
    }

    override fun onDisable() {
        super.onDisable()
        MinecraftForge.EVENT_BUS.unregister(BrushBuildTools)
    }

    override fun onEnable() {
        super.onEnable()
        MinecraftForge.EVENT_BUS.register(BrushBuildTools)
        if (mc.thePlayer != null) reload()
    }

    fun reload(){
        savedChunks.clear()
        registerLocation(DungeonUtils.currentRoom)
        val loadedChunks = Core.mc.theWorld?.chunkProvider as ChunkListingFieldAccessor
        loadedChunks.chunkList.forEach { loadedChunk ->
            onChunkLoad(loadedChunk)
        }
    }

    fun calculateChunkHash(x: Int, z: Int): Long {
        return x.toLong() and 4294967295L or ((z.toLong() and 4294967295L) shl 32)
    }

    fun onChunkLoad(chunk: Chunk) {
        if (!enabled) return
        val blocks = savedChunks.get(calculateChunkHash(chunk))
        if (blocks == null) return

        blocks.forEach { (pos, state) ->
            setBlock(pos, state)
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        editMode = false
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        savedChunks.clear()
        if (editMode) {
            editMode = false
            saveConfig()
        }
    }
    fun toggleEditMode(){
        editMode = !editMode
        if (!editMode) saveConfig()
        modMessage("Toggled Edit Mode: ${if (editMode) "§l§aOn" else "§l§cOff"}")
    }

    private inline val canPlaceBlock get() =
        System.currentTimeMillis() - lastPlace >= placeCooldown &&
        (getEditingBlockState() ?: IBlockStateUtils.airIBlockState) != IBlockStateUtils.airIBlockState


    private fun handlePlaceBlock(pos: BlockPos, hitVec: Vec3, facing: EnumFacing, room: UniqueRoom?){
        lastPlace = System.currentTimeMillis()
        val blockState = getEditingBlockState() ?: return
        val state = blockState.block.onBlockPlaced(
            mc.theWorld,
            pos,
            facing,
            hitVec.xCoord.toFloat(),
            hitVec.yCoord.toFloat(),
            hitVec.zCoord.toFloat(),
            blockState.block.getMetaFromState(blockState),
            mc.thePlayer
        )
        val blockList = getBlockList(room)
        val blockToAdd = Pair(state , room?.getRealCoords(pos) ?: pos)
        blockList.removeAll { it.second.x == blockToAdd.second.x && it.second.y == blockToAdd.second.y && it.second.z == blockToAdd.second.z }
        blockList.add(blockToAdd)
        setBlock(pos, state)
        removeBlockFromChunk(pos)
    }

    fun getEditingBlockState(): IBlockState? {
        if (hotbarPalette) {
            val held = mc.thePlayer.heldItem
            val block = Block.getBlockFromItem(held.item) ?: return null
            return block.getStateFromMeta(held.metadata)
        }
        return selectedBlockState
    }

    fun getBlockList(room: UniqueRoom?): MutableList<Pair<IBlockState, BlockPos>>{
        return if (room != null) {
            roomConfig.getOrPut(room.name) { mutableListOf() }
        } else {
            floorConfig.getOrPut(getLocation()) { mutableListOf() }
        }
    }

    @SubscribeEvent
    fun mouseEvent(event: InputEvent.MouseInputEvent){
        if (Mouse.getEventButton() != 1 || Mouse.getEventButtonState()) return
        lastRight = false
    }

    private var lastRight = false
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onMouse(event: ClickEvent.All) {
        if (!editMode) return
        val mouseOver = if (FreeCam.enabled) FreeCam.looking else mc.objectMouseOver ?: return
        val target = mouseOver?.hitBlock() ?: return
        val room = DungeonUtils.currentRoom
        event.isCanceled = true

        val axeHeld = mc.thePlayer.heldItem.ID == 271

        if (event.type == ClickEvent.ClickType.Right && !lastRight) {
            if (axeHeld) {
                lastRight = true

                BrushBuildTools.rightBlockPos = target
                modMessage("Right:§r§d§l $target")
                return
            }
            if (!canPlaceBlock) return

            val facing = mouseOver.sideHit
            val offset = target.offset(facing)
            val hitVec = mouseOver.hitBlockVec() ?: return
            handlePlaceBlock(offset, hitVec, facing, room)

        }

        if (event.type == ClickEvent.ClickType.Middle) {
            selectedBlockState = getBlockStateAt(target)
            modMessage(
                "Selected block state: §l§a${
                    selectedBlockState.block.registryName.removePrefix("minecraft:").capitalizeFirst().replace("_", " ")
                }"
            )
            return
        }


        if (event.type == ClickEvent.ClickType.Left) {
            if (axeHeld) {
                BrushBuildTools.leftBlockPos = target
                modMessage("Left:§r§d§l $target")
                return
            }

            val blockList = getBlockList(room)
            val pos = room?.getRelativeCoords(target) ?: target

            val removed = blockList.removeAll{ (_, blockPos) ->
                pos == blockPos
            }
            addBlockToChunk(pos, IBlockStateUtils.airIBlockState)

            if (removed) return
            blockList.add(IBlockStateUtils.airIBlockState to pos)

            return
        }
    }

    fun addBlockToChunk(target: BlockPos, state: IBlockState) {
        runOnMCThread {
            val hash = calculateChunkHash(target.x shr 4, target.z shr 4)
            savedChunks.getOrPut(hash) { hashMapOf() }.put(target, state)
            setBlock(target, state)
        }
    }

    fun removeBlockFromChunk(target: BlockPos){
        val hash = calculateChunkHash(target.x shr 4, target.z shr 4)
        savedChunks.getOrElse(hash) { null }?.remove(target) ?: return
    }

    fun registerLocation(room: UniqueRoom?){
        registerChunkBlocks(room, floorConfig[room?.name ?: getLocation()] ?: return)
    }

    @SubscribeEvent
    fun locationEvent(event: LocationChangeEvent){
        registerLocation(null)
    }

    @SubscribeEvent
    fun onRoomEnterEvent(event: RoomEnterEvent){
        if (event.room == null) return
        registerLocation(event.room)
    }

    fun registerChunkBlocks(room: UniqueRoom?, positions: List<Pair<IBlockState, BlockPos>>) {
        runOnMCThread {
            positions.forEach { (state, localPos) ->
                val pos = room?.getRealCoords(localPos) ?: localPos

                val hash = calculateChunkHash(pos.x shr 4, pos.z shr 4)
                if (room != null && mc.theWorld.getChunkFromChunkCoords(pos.x shr 4, pos.z shr 4).isLoaded) {
                    setBlock(pos, state)
                }
                savedChunks.getOrPut(hash) { hashMapOf() }.put(pos, state)
            }
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

    private fun MovingObjectPosition.hitBlockVec(): Vec3? {
        return if (this.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) this.hitVec.subtract(this.blockPos.toVec3()) else null
    }

    private fun getLocation(): String {
        val location = LocationUtils.currentArea
        return when (location) {
            Island.Dungeon -> {
                "${DungeonUtils.floorNumber}"
            }
            Island.SinglePlayer -> {
                "7"
            }
            else -> location.displayName
        }
    }

    fun loadConfig() {
        val floorConfigFile = DataManager.loadDataFromFileObjectOfObjects("floorConfig")
        val roomConfigFile = DataManager.loadDataFromFileObjectOfObjects("roomConfig")
        floorConfig.clear()
        roomConfig.clear()
        savedChunks.clear()
        loadAreaConfig(floorConfigFile, floorConfig)
        loadAreaConfig(roomConfigFile, roomConfig)
    }

    private fun loadAreaConfig(configFile: Map<String, JsonObject>, config: ConcurrentHashMap<String, MutableList<Pair<IBlockState, BlockPos>>>){
        for ((area, blocks) in configFile) {
            val blockList = mutableListOf<Pair<IBlockState, BlockPos>>()
            val areaObject = blocks.asJsonObject

            for ((key, jsonArray) in areaObject.entrySet()) {
                val state = getBlockStateFromString(key)

                for (posElement in jsonArray.asJsonArray) {
                    blockList.add(state to posElement.asBlockPos)
                }
            }
            config[area] = blockList
        }
    }

    private fun saveAreaConfig(config: ConcurrentHashMap<String, MutableList<Pair<IBlockState, BlockPos>>>, name: String) {
        val root = JsonObject()

        for ((area, blocks) in config) {
            val areaObject = JsonObject()

            val blockSudoMap = mutableMapOf<String, MutableList<BlockPos>>()
            for ((state, pos) in blocks) {
                blockSudoMap.getOrPut(state.toString()) { ArrayList() }.add(pos)
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
        DataManager.saveDataToFile(name, root)
    }


    fun saveConfig() {
        saveAreaConfig(floorConfig, "floorConfig")
        saveAreaConfig(roomConfig, "roomConfig")
    }


    private fun blockStateSerializer(state: IBlockState): String {
        val blockName = Block.blockRegistry.getNameForObject(state.block)?.toString() ?: return "minecraft:air"
        if (state.properties.isEmpty()) return blockName
        val propString = state.properties.entries.joinToString(",") { (key, value) ->
            "${key.name}=${key.getName(value)}"
        }
        return "$blockName[$propString]"
    }

    private fun getBlockStateFromString(raw: String): IBlockState {
        val hasArgs = raw.contains("[") && raw.contains("]")

        if (!hasArgs) {
            val block = Block.getBlockFromName(raw)
            return block?.defaultState ?: IBlockStateUtils.airIBlockState
        }
        val baseName = raw.substringBefore("[")
        val block = Block.getBlockFromName(baseName)
        var state: IBlockState = block?.defaultState ?: IBlockStateUtils.airIBlockState

        val inner = raw.substringAfter("[").substringBefore("]")
        val args = inner.split(",")

        for (arg in args) {
            val parts = arg.split("=", limit = 2)
            if (parts.size != 2) continue
            val key = parts[0]
            val value = parts[1]
            val prop = state.propertyNames
                .firstOrNull { it.name == key } as? IProperty<*>
                ?: continue

            state = state.withProperty(prop, value)
        }

        return state
    }
}