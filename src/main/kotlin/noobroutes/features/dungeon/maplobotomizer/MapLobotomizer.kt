package noobroutes.features.dungeon.maplobotomizer


import net.minecraft.block.state.BlockState
import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.BossEventDispatcher
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.utils.setBlock
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import org.lwjgl.input.Keyboard


object MapLobotomizer : Module("Map Lobotomizer", description = "It is just fme but way less laggy.", category = Category.DUNGEON) {
    var editMode by BooleanSetting("Edit Mode", description = "Allows you to edit blocks")
    val editModeToggle by KeybindSetting("Edit Mode Bind", Keyboard.KEY_NONE, description = "Toggles Edit Mode").onPress { editMode = !editMode }
    class Block(val pos: BlockPos, val state: BlockState)

    var blocks:  MutableMap<String, MutableSet<Block>> = mutableMapOf()


    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onMouse(event: MouseEvent) {
        if (!editMode) return
        val target = mc.objectMouseOver?.blockPos ?: return
        val location = getLocation()
        val room = DungeonUtils.currentRoom

        if (event.button == 0 && event.buttonstate) {
            //setBlock(target, IBlockState)
        }


    }

    fun getLocation(): String {
        val room = DungeonUtils.currentRoom?.data?.name ?: "Unknown"
        if (room != "Unknown") return room
        val location = LocationUtils.currentArea
        if (location.name == "Catacombs") {
            return "Floor ${LocationUtils.currentDungeon?.floor?.floorNumber}"
        }
        return location.name
    }





}