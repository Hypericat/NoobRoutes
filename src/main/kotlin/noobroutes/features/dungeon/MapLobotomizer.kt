package noobroutes.features.dungeon


import net.minecraft.block.state.BlockState
import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.utils.IBlockStateUtils
import noobroutes.utils.removeFirstOrNull
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import org.lwjgl.input.Keyboard


object MapLobotomizer : Module("Map Lobotomizer", description = "It is just fme but way less laggy.", category = Category.DUNGEON) {
    private var editMode by BooleanSetting("Edit Mode", description = "Allows you to edit blocks")
    val editModeToggle by KeybindSetting("Edit Mode Bind", Keyboard.KEY_NONE, description = "Toggles Edit Mode").onPress { editMode = !editMode }
    class Block(val pos: BlockPos, val state: IBlockState)

    var blocks:  MutableMap<String, MutableSet<Block>> = mutableMapOf()
    var blockState: IBlockState = IBlockStateUtils.airIBlockState

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onMouse(event: MouseEvent) {
        if (!editMode || !event.buttonstate) return
        val target = mc.objectMouseOver?.blockPos ?: return
        val room = DungeonUtils.currentRoom
        event.isCanceled = true

        if (event.button == 0) {

            val pos = room?.getRelativeCoords(target) ?: target
            val blockList = blocks.getOrPut(room?.data?.name ?: getLocation()) { mutableSetOf() }
            val removed = blockList.removeFirstOrNull {
                it.pos == pos
            }
            if (removed != null) return
            blockList.add(Block(pos, IBlockStateUtils.airIBlockState))
            return
        }
        if (event.button == 2) {
            blockState
        }



    }

    private fun getLocation(): String {
        val location = LocationUtils.currentArea
        if (location.name == "Catacombs") {
            return "Floor ${LocationUtils.currentDungeon?.floor?.floorNumber}"
        }
        return location.name
    }





}