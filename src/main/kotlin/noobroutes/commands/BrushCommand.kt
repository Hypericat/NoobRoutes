package noobroutes.commands

import net.minecraft.block.Block
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos
import noobroutes.Core
import noobroutes.features.dungeon.Brush
import noobroutes.features.dungeon.Brush.editMode
import noobroutes.ui.blockgui.BlockGui
import noobroutes.utils.IBlockStateUtils
import noobroutes.utils.skyblock.modMessage

class BrushCommand: CommandBase() {
    override fun getCommandName(): String {
        return "brush"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "to utilize the brush feature"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        if (args == null || args.isEmpty()) return modMessage("Usages: Add, Delete, Set, Clear, Load, LoadFunnyMap")
        when (args[0].lowercase()) {
            "em", "e", "edit" -> {
                Brush.toggleEditMode()
            }
            "bg", "g", "gui" -> {
                Core.display = BlockGui
            }
            "load", "l" -> {
                Brush.loadConfig()
                modMessage("Loaded Config")
            }
            "fill", "f" -> {
                if (!editMode) return modMessage("Edit Mode Required")
                val state = Brush.getEditingBlockState()
                if (state == IBlockStateUtils.airIBlockState || state == null) return modMessage("Selected Block State Required")
                val selectedArea = Brush.getSelectedArea()
                if (args.size < 2) {
                    Brush.fill(selectedArea, state)
                    return
                }
                Brush.filteredFill(selectedArea, getBlockByText(sender, args[1]), state)
            }

            "clear", "c" -> {
                if (!editMode) return modMessage("Edit Mode Required")
                val selectedArea = Brush.getSelectedArea()
                if (args.size < 2) {
                    Brush.fill(selectedArea, IBlockStateUtils.airIBlockState)
                    return
                }
                Brush.filteredFill(selectedArea, getBlockByText(sender, args[1]), IBlockStateUtils.airIBlockState)
            }
            else -> modMessage("Usages: Edit, Gui, Load, Fill, Clear")
        }
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }
    override fun getCommandAliases(): List<String> {
        return listOf("br")
    }

    override fun addTabCompletionOptions(
        sender: ICommandSender?,
        args: Array<String?>,
        pos: BlockPos?
    ): MutableList<String?>? {
        return when (args.size) {
            0 -> {
                mutableListOf("Edit", "Gui", "Load", "Fill", "Clear")
            }
            1 -> {
                getListOfStringsMatchingLastWord(
                    args,
                    Block.blockRegistry.getKeys()
                )
            }
            else -> null
        }
    }

}