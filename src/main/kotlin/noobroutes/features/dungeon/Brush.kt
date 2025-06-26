package noobroutes.features.dungeon

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.block.Block
import net.minecraft.block.properties.IProperty
import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraft.world.chunk.Chunk
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
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
import noobroutes.utils.IBlockStateUtils.withProperty
import noobroutes.utils.IBlockStateUtils.withRotation
import noobroutes.utils.capitalizeFirst
import noobroutes.utils.getBlockStateAt
import noobroutes.utils.json.JsonUtils.add
import noobroutes.utils.json.JsonUtils.asBlockPos
import noobroutes.utils.runOnMCThread
import noobroutes.utils.setBlock
import noobroutes.utils.skyblock.Island
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.toVec3
import org.lwjgl.input.Keyboard
import kotlin.collections.iterator

object Brush : Module("Brush", description = "It is just fme but way less laggy. Works with FME floor config, but not the room config.", category = Category.DUNGEON) {


    init {
        execute(1) {
            if (!editMode || !mc.gameSettings.keyBindUseItem.isKeyDown || System.currentTimeMillis() - lastPlace < placeCooldown || selectedBlockState == IBlockStateUtils.airIBlockState) return@execute
            val mouseOver = if (FreeCam.enabled) FreeCam.looking else mc.objectMouseOver ?: return@execute
            val target = mouseOver?.hitBlock() ?: return@execute
            val room = DungeonUtils.currentRoom
            lastPlace = System.currentTimeMillis()
            val facing = mouseOver.sideHit
            val offset = target.offset(facing)
            val pos = room?.getRealCoords(offset) ?: offset
            val hitVec = mouseOver.hitBlockVec() ?: return@execute
            val state = selectedBlockState.block.onBlockPlaced(
                mc.theWorld,
                offset,
                facing,
                hitVec.xCoord.toFloat(),
                hitVec.yCoord.toFloat(),
                hitVec.zCoord.toFloat(),
                selectedBlockState.block.getMetaFromState(selectedBlockState),
                mc.thePlayer
            )
            val blockList = if (room != null) {
                roomConfig.getOrPut(room.name) { mutableListOf() }
            } else {
                floorConfig.getOrPut(getLocation()) { mutableListOf() }
            }

            val blockToAdd = Pair(state , pos)
            blockList.removeAll { it.second.x == blockToAdd.second.x && it.second.y == blockToAdd.second.y && it.second.z == blockToAdd.second.z }
            blockList.add(blockToAdd)

            runOnMCThread {
                val hash = Pair(offset.x shr 4, offset.z shr 4)
                savedChunks.getOrPut(hash) { hashMapOf() }.put(offset, state )
                setBlock(offset, state );
            }
        }

    }


    var editMode by BooleanSetting("Edit Mode", description = "Allows you to edit blocks")
    val editModeToggle by KeybindSetting("Edit Mode Bind", Keyboard.KEY_NONE, description = "Toggles Edit Mode").onPress {
        toggleEditMode()
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
    var floorConfig: MutableMap<String, MutableList<Pair<IBlockState, BlockPos>>> = mutableMapOf()
    var roomConfig: MutableMap<String, MutableList<Pair<IBlockState, BlockPos>>> = mutableMapOf()



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


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onMouse(event: ClickEvent.All) {
        if (!editMode) return
        val mouseOver = if (FreeCam.enabled) FreeCam.looking else mc.objectMouseOver ?: return
        val target = mouseOver?.hitBlock() ?: return
        val room = DungeonUtils.currentRoom
        event.isCanceled = true

        if (event.type == ClickEvent.ClickType.Middle) {
            selectedBlockState = getBlockStateAt(target)
            modMessage(
                "Selected block state: §l§a${
                    selectedBlockState.block.registryName.removePrefix("minecraft:").capitalizeFirst().replace("_", " ")
                }"
            )
            return
        }
        val blockList = if (room != null) {
            roomConfig.getOrPut(room.name) { mutableListOf() }
        } else {
            floorConfig.getOrPut(getLocation()) { mutableListOf() }
        }



        if (event.type == ClickEvent.ClickType.Left) {
            val pos = room?.getRelativeCoords(target) ?: target

            val removed = blockList.removeAll{ (_, blockPos) ->
                pos == blockPos;
            }

            if (removed) {
                runOnMCThread {
                    val hash = Pair(target.x shr 4, target.z shr 4)
                    savedChunks.getOrElse(hash) { null }?.remove(target) ?: return@runOnMCThread
                    setBlock(target, IBlockStateUtils.airIBlockState);
                }
                return
            }

            blockList.add(Pair(IBlockStateUtils.airIBlockState, pos))
            runOnMCThread {
                val hash = Pair(target.x shr 4, target.z shr 4)
                savedChunks.getOrPut(hash) { hashMapOf() }.put(target, IBlockStateUtils.airIBlockState)
                setBlock(target, IBlockStateUtils.airIBlockState);
            }
            return;
        }


    }





    @SubscribeEvent
    fun locationEvent(event: LocationChangeEvent){
        registerChunkBlocks(null, floorConfig[getLocation()] ?: return)
    }

    @SubscribeEvent
    fun onRoomEnterEvent(event: RoomEnterEvent){
        if (event.room == null) return
        val list = roomConfig[event.room.name] ?: return

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

    private fun loadAreaConfig(configFile: Map<String, JsonObject>, config: MutableMap<String, MutableList<Pair<IBlockState, BlockPos>>>){
        for ((area, blocks) in configFile) {
            val blockList = mutableListOf<Pair<IBlockState, BlockPos>>()
            val areaObject = blocks.asJsonObject

            for ((key, jsonArray) in areaObject.entrySet()) {
                val state = getBlockState(key)

                for (posElement in jsonArray.asJsonArray) {
                    blockList.add(state to posElement.asBlockPos)
                }
            }
            config[area] = blockList
        }
    }

    private fun saveAreaConfig(config: MutableMap<String, MutableList<Pair<IBlockState, BlockPos>>>, name: String) {
        val root = JsonObject()

        for ((area, blocks) in config) {
            val areaObject = JsonObject()

            val blockSudoMap = mutableMapOf<String, MutableList<BlockPos>>()
            for ((state, pos) in blocks) {
                blockSudoMap.getOrPut(blockStateSerializer(state)) { ArrayList() }.add(pos)
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

    private fun getBlockState(raw: String): IBlockState {
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