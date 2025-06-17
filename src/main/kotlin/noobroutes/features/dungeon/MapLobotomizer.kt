package noobroutes.features.dungeon


import net.minecraft.block.state.IBlockState
import net.minecraft.network.play.server.S21PacketChunkData
import net.minecraft.network.play.server.S22PacketMultiBlockChange
import net.minecraft.network.play.server.S23PacketBlockChange
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.events.impl.ClickEvent
import noobroutes.events.impl.LocationChangeEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.RoomEnterEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.render.FreeCam
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.IBlockStateUtils
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.getBlockStateAt
import noobroutes.utils.removeFirstOrNull
import noobroutes.utils.setBlock
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeonScanning.DungeonUtils
import noobroutes.utils.skyblock.dungeonScanning.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeonScanning.DungeonUtils.getRelativeCoords
import org.lwjgl.input.Keyboard


object MapLobotomizer : Module("Map Lobotomizer", description = "It is just fme but way less laggy.", category = Category.DUNGEON) {
    var editMode by BooleanSetting("Edit Mode", description = "Allows you to edit blocks")
    val editModeToggle by KeybindSetting("Edit Mode Bind", Keyboard.KEY_NONE, description = "Toggles Edit Mode").onPress { editMode = !editMode }
    val placeCooldown by NumberSetting("Place Cooldown", min = 0, max = 1000, default = 150,  description = "Cooldown between placing blocks in edit mode", unit = "ms")
    data class Block(val pos: BlockPos, val state: IBlockState)

    var pendingBlocks = mutableSetOf<Block>()
    var blocks:  MutableMap<String, MutableSet<Block>> = mutableMapOf()
    var selectedBlockState: IBlockState = IBlockStateUtils.airIBlockState

    private var lastPlace = System.currentTimeMillis()

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
        val blockList = blocks.getOrPut(room?.name ?: getLocation()) { mutableSetOf() }
        if (event.type == ClickEvent.ClickType.Left) {
            val pos = room?.getRelativeCoords(target) ?: target
            val removed = blockList.removeFirstOrNull {
                it.pos == pos
            }
            setBlock(pos, IBlockStateUtils.airIBlockState)
            if (removed != null) return
            blockList.add(Block(pos, IBlockStateUtils.airIBlockState))
            return
        }
        if (event.type == ClickEvent.ClickType.Right) {
            if (System.currentTimeMillis() - lastPlace < placeCooldown || selectedBlockState == IBlockStateUtils.airIBlockState) return
            lastPlace = System.currentTimeMillis()
            val facing = mouseOver.sideHit
            val offset = target.offset(facing)
            val pos = room?.getRelativeCoords(target) ?: target
            setBlock(offset, selectedBlockState)
            blockList.add(Block(pos, selectedBlockState))
        }
    }

    @SubscribeEvent
    fun locationEvent(event: LocationChangeEvent){
        val location = if (event.location.displayName == "Catacombs")
            "Floor ${DungeonUtils.floorNumber}" else event.location.displayName
        pendingBlocks.addAll(blocks[location] ?: return)
    }

    @SubscribeEvent
    fun onRoomEnterEvent(event: RoomEnterEvent){
        if (event.room == null) return
        pendingBlocks.addAll(
            blocks[event.room.name]?.map { Block(event.room.getRealCoords(it.pos), it.state) } ?: return
        )
        devMessage("something")
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent){
        if (event.isEnd) {
            return
        }
        if (pendingBlocks.isEmpty()) return
        val iterator = pendingBlocks.iterator()
        val world = mc.theWorld
        while (iterator.hasNext()) {
            val block = iterator.next()
            if (world.getChunkFromBlockCoords(block.pos).isLoaded) {
                devMessage(block.pos)
                setBlock(block.pos, block.state)
                iterator.remove()
            }
        }
    }



    @SubscribeEvent
    fun onPacket(event: PacketEvent){
        val room = DungeonUtils.currentRoom
        if (event.packet is S23PacketBlockChange) {
            val position = room?.getRelativeCoords(event.packet.blockPosition) ?: event.packet.blockPosition
            val block = blocks[room?.name ?: getLocation()]?.firstOrNull {
                it.pos == position
            } ?: return
            setBlock(event.packet.blockPosition, block.state)
            event.isCanceled = true
        }
        if (event.packet is S22PacketMultiBlockChange) {
            val blockList = blocks[room?.name ?: getLocation()] ?: return
            event.packet.changedBlocks.forEach { changedBlock ->
                val block = blockList.firstOrNull{ it.pos == (room?.getRealCoords(changedBlock.pos) ?: changedBlock.pos) }
                if (block == null) {
                    setBlock(changedBlock.pos, changedBlock.blockState)
                    return@forEach
                }
                setBlock(changedBlock.pos, block.state)
            }
            event.isCanceled = true
        }
        if (room != null) return
        if (event.packet is S21PacketChunkData) {
            event.packet.chunkZ
            val minX = event.packet.chunkX * 16
            val minZ = event.packet.chunkZ * 16
            val maxX = minX + 15
            val maxZ = minZ + 15
            devMessage(blocks[getLocation()]?.filter {
                it.pos.x in minX..maxX && it.pos.z in minZ..maxZ
            }?.size)
            pendingBlocks.addAll(
                blocks[getLocation()]?.filter {
                    it.pos.x in minX..maxX && it.pos.z in minZ..maxZ
                } ?: return
            )
        }

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
}