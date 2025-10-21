package noobroutes.features.dungeon.brush

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.block.Block
import net.minecraft.block.properties.IProperty
import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
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
import noobroutes.features.settings.DevOnly
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.mixin.accessors.ChunkListingFieldAccessor
import noobroutes.utils.*
import noobroutes.utils.IBlockStateUtils.withProperty
import noobroutes.utils.json.JsonUtils.add
import noobroutes.utils.json.JsonUtils.asBlockPos
import noobroutes.utils.skyblock.Island
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import java.util.function.Predicate


@Suppress("unused")
@DevOnly
object BrushModule : Module("Brush", description = "Brush, allows editing dungeon breaker aura.", category = Category.DUNGEON) {
    var editMode by BooleanSetting("Edit Mode", description = "Allows you to edit blocks")
    private val editModeToggle by KeybindSetting(
        "Edit Mode Bind",
        Keyboard.KEY_NONE,
        description = "Toggles Edit Mode"
    ).onPress {
        toggleEditMode()
    }

    private var savedChunks = hashMapOf<Long, HashSet<BlockPos>>()
    private val floorConfig = ConcurrentHashMap<String, HashSet<BlockPos>>()
    private val roomConfig = ConcurrentHashMap<String, HashSet<BlockPos>>()

    private var forceF7 by BooleanSetting("Force F7", description = "Forces Brush to think its in f7 even on servers")
    private var simulate by BooleanSetting("Simulate", description = "Creates ghost blocks in singleplayer")


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

    fun getBlockList(room: UniqueRoom?): HashSet<BlockPos>{
        return if (room != null) {
            roomConfig.getOrPut(room.name) { hashSetOf() }
        } else {
            floorConfig.getOrPut(getLocation()) { hashSetOf() }
        }
    }

    private var lastRight = false
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onMouse(event: ClickEvent.All) {
        if (!editMode) return
        val mouseOver = if (FreeCam.enabled) FreeCam.looking else mc.objectMouseOver ?: return
        val target = mouseOver?.hitBlock() ?: return
        val room = DungeonUtils.currentRoom
        event.isCanceled = true

        if (event.type == ClickEvent.ClickType.Left) {
            val blockList = getBlockList(room)
            val pos = room?.getRelativeCoords(target) ?: target

            //removeBlockFromChunk(target)

            //val removed = blockList.remove(pos);
            //if (removed) return

            addBlockToChunk(target)
            blockList.add(pos)
            if (simulate && Minecraft.getMinecraft().isSingleplayer)
                setBlock(target, Blocks.air.defaultState)
            return;
        }

        if (event.type == ClickEvent.ClickType.Right) {
            val blockList = getBlockList(room)
            val pos = room?.getRelativeCoords(target) ?: target

            removeBlockFromChunk(target)
            val removed = blockList.remove(pos);
            if (!removed) return

            setBlock(target, Blocks.stone.defaultState)
        }
        return
    }

    fun addBlockToChunk(target: BlockPos) {
        runOnMCThread {
            val hash = calculateChunkHash(target.x shr 4, target.z shr 4)
            savedChunks.getOrPut(hash) { hashSetOf() }.add(target)
        }
    }

    fun removeBlockFromChunk(target: BlockPos){
        val hash = calculateChunkHash(target.x shr 4, target.z shr 4)
        savedChunks.getOrElse(hash) { null }?.remove(target) ?: return
    }

    fun registerLocation(room: UniqueRoom?){
        if (room != null) {
            registerChunkBlocks(room, roomConfig[room.name] ?: return)
            return
        }
        registerChunkBlocks(null, floorConfig[getLocation()] ?: return)
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

    fun registerChunkBlocks(room: UniqueRoom?, positions: Collection<BlockPos>) {
        runOnMCThread {
            positions.forEach { localPos ->
                val pos: BlockPos = room?.getRealCoords(localPos) ?: localPos
                val hash = calculateChunkHash(pos.x shr 4, pos.z shr 4)
                savedChunks.getOrPut(hash) { hashSetOf() }.add(pos)
            }
        }
    }

    fun isBlockEdited(blockPos: BlockPos): Boolean? {
        val hash = calculateChunkHash(blockPos.x shr 4, blockPos.z shr 4)
        val blocks = savedChunks[hash] ?: return null
        return blocks.contains(blockPos)
    }


    private fun MovingObjectPosition.hitBlock(): BlockPos? {
        return if (this.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) this.blockPos else null
    }

    private fun MovingObjectPosition.hitBlockVec(): Vec3? {
        return if (this.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) this.hitVec.subtract(this.blockPos.toVec3()) else null
    }

    private fun getLocation(): String {
        if (forceF7) return "7"
        val location = LocationUtils.currentArea
        return when (location) {
            Island.Dungeon -> {
                DungeonUtils.floorNumber.toString()
            }
            Island.SinglePlayer -> {
                "7"
            }
            else -> location.displayName
        }
    }

    fun loadConfig() {
        val floorConfigFile = DataManager.loadDataFromFileObjectOfObjects("floorsConfig")
        val roomConfigFile = DataManager.loadDataFromFileObjectOfObjects("roomConfig")
        floorConfig.clear()
        roomConfig.clear()
        savedChunks.clear()
        loadAreaConfig(floorConfigFile, floorConfig)
        loadAreaConfig(roomConfigFile, roomConfig)
    }

    private fun loadAreaConfig(configFile: Map<String, JsonObject>, config: ConcurrentHashMap<String, HashSet<BlockPos>>){
        for ((area, blocks) in configFile) {
            val blockList = hashSetOf<BlockPos>();
            val areaObject = blocks.asJsonObject

            for ((key, jsonArray) in areaObject.entrySet()) {
                for (posElement in jsonArray.asJsonArray) {
                    blockList.add(posElement.asBlockPos)
                }
            }
            config[area] = blockList
        }
    }

    private fun saveAreaConfig(config: ConcurrentHashMap<String, HashSet<BlockPos>>, name: String) {
        val root = JsonObject()

        for ((area, blocks) in config) {
            val areaObject = JsonObject()
            val array = JsonArray()
            blocks.forEach {
                array.add(it)
            }

            areaObject.add(area, array);
            root.add(area, areaObject)
        }
        DataManager.saveDataToFile(name, root)
    }

    fun clear() {
        getBlockList(DungeonUtils.currentRoom).clear();
    }

    fun consumeBlocks(consumer: Consumer<BlockPos>) {
        val room = DungeonUtils.currentRoom;
        getBlockList(room).forEach { pos ->
            consumer.accept(room?.getRealCoords(pos) ?: pos);
        }
    }

    fun predicateFirstBlock(consumer: Predicate<BlockPos>) {
        val room = DungeonUtils.currentRoom;
        getBlockList(room).forEach { pos ->
            if (consumer.test(room?.getRealCoords(pos) ?: pos)) return;
        }
    }


    fun saveConfig() {
        saveAreaConfig(floorConfig, "floorsConfig")
        saveAreaConfig(roomConfig, "roomConfig")
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
                .firstOrNull { it.name == key } as IProperty<*>
            state = state.withProperty(prop, value)
        }

        return state
    }
}