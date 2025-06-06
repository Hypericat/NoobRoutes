package noobroutes.features.dungeon


import net.minecraft.block.state.IBlockState
import net.minecraft.network.play.server.S22PacketMultiBlockChange
import net.minecraft.network.play.server.S23PacketBlockChange
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.event.world.ChunkEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.impl.LocationChangeEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.RoomEnterEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.render.FreeCam
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.utils.IBlockStateUtils
import noobroutes.utils.getBlockStateAt
import noobroutes.utils.removeFirstOrNull
import noobroutes.utils.setBlock
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import org.lwjgl.input.Keyboard


object MapLobotomizer : Module("Map Lobotomizer", description = "It is just fme but way less laggy.", category = Category.DUNGEON) {
    private var editMode by BooleanSetting("Edit Mode", description = "Allows you to edit blocks")
    val editModeToggle by KeybindSetting("Edit Mode Bind", Keyboard.KEY_NONE, description = "Toggles Edit Mode").onPress { editMode = !editMode }
    class Block(val pos: BlockPos, val state: IBlockState)

    var blocks:  MutableMap<String, MutableSet<Block>> = mutableMapOf()
    var selectedBlockState: IBlockState = IBlockStateUtils.airIBlockState

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onMouse(event: MouseEvent) {
        if (!editMode || !event.buttonstate) return
        val mouseOver = if (FreeCam.enabled) FreeCam.looking else mc.objectMouseOver ?: return
        val target = mouseOver?.hitBlock() ?: return
        val room = DungeonUtils.currentRoom
        event.isCanceled = true

        if (event.button == 2) {
            selectedBlockState = getBlockStateAt(target)
        }
        val blockList = blocks.getOrPut(room?.data?.name ?: getLocation()) { mutableSetOf() }
        if (event.button == 0) {
            val pos = room?.getRelativeCoords(target) ?: target
            val removed = blockList.removeFirstOrNull {
                it.pos == pos
            }
            if (removed != null) return
            setBlock(pos, IBlockStateUtils.airIBlockState)
            blockList.add(Block(pos, IBlockStateUtils.airIBlockState))
            return
        }
        if (event.button == 1) {
            val facing = mouseOver.sideHit
            val offset = target.offset(facing)
            setBlock(offset, selectedBlockState)
            blockList.add(Block(offset, selectedBlockState))
        }
    }

    @SubscribeEvent
    fun locationEvent(event: LocationChangeEvent){
        val location = if (event.location.displayName == "Catacombs")
            "Floor ${DungeonUtils.floorNumber}" else event.location.displayName
        blocks[location]?.forEach {
            setBlock(it.pos, it.state)
        }
    }

    @SubscribeEvent
    fun onRoomEnterEvent(event: RoomEnterEvent){
        if (event.room == null) return
        blocks[event.room.data.name]?.forEach {
            setBlock(event.room.getRealCoords(it.pos), it.state)
        }
    }

    @SubscribeEvent
    fun onChunkLoad(event: ChunkEvent.Load){
        if (DungeonUtils.currentRoom != null) return
        val minX = event.chunk.xPosition * 16
        val minZ = event.chunk.zPosition * 16
        val maxX = minX + 15
        val maxZ = minZ + 15
        blocks[getLocation()]?.filter {
            it.pos.x in minX..maxX && it.pos.z in minZ..maxZ
        }?.forEach {
            setBlock(it.pos, it.state)
        }
    }


    @SubscribeEvent
    fun onBlockChangePacket(event: PacketEvent){
        val room = DungeonUtils.currentRoom
        if (event.packet is S23PacketBlockChange) {
            val position = room?.getRelativeCoords(event.packet.blockPosition) ?: event.packet.blockPosition
            val block = blocks[room?.data?.name ?: getLocation()]?.firstOrNull {
                it.pos == position
            } ?: return
            setBlock(event.packet.blockPosition, block.state)
            event.isCanceled = true
        }
        if (event.packet is S22PacketMultiBlockChange) {
            val blockList = blocks[room?.data?.name ?: getLocation()] ?: return
            event.packet.changedBlocks.forEach { changedBlock ->
                val block = blockList.firstOrNull{ it.pos == (room?.getRelativeCoords(changedBlock.pos) ?: changedBlock.pos) }
                if (block == null) {
                    setBlock(changedBlock.pos, changedBlock.blockState)
                    return@forEach
                }
                setBlock(changedBlock.pos, block.state)
            }
            event.isCanceled = true
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