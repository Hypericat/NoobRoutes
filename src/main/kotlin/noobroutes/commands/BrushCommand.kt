package noobroutes.commands

import net.minecraft.block.Block
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos
import noobroutes.Core
import noobroutes.features.dungeon.brush.BrushBuildTools
import noobroutes.features.dungeon.brush.BrushModule
import noobroutes.features.dungeon.brush.BrushModule.editMode
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
                BrushModule.toggleEditMode()
            }
            "bg", "g", "gui" -> {
                Core.display = BlockGui
            }
            "load", "l" -> {
                BrushModule.loadConfig()
                modMessage("Loaded Config")
            }
            "fill", "f" -> {
                if (!editMode) return modMessage("Edit Mode Required")
                val state = BrushModule.getEditingBlockState()
                if (state == IBlockStateUtils.airIBlockState || state == null) return modMessage("Selected Block State Required")
                val selectedArea = BrushBuildTools.getSelectedArea()
                modMessage("§l§aFilling $state")
                if (args.size < 2) {
                    val thread = Thread { BrushBuildTools.fill(selectedArea, state) }
                    thread.start()
                    return
                }
                val thread = Thread { BrushBuildTools.filteredFill(selectedArea, getBlockByText(sender, args[1]), state) }
                thread.start()

            }

            "reload", "r" -> {
                BrushModule.reload()
            }

            "undo" -> {
                BrushBuildTools.handleUndo()
            }

            "clear", "c" -> {
                if (!editMode) return modMessage("Edit Mode Required")
                modMessage("§l§aClearing")
                val selectedArea = BrushBuildTools.getSelectedArea()
                if (args.size < 2) {
                    val thread = Thread { BrushBuildTools.fill(selectedArea, IBlockStateUtils.airIBlockState) }
                    thread.start()
                    return
                }
                val thread = Thread { BrushBuildTools.filteredFill(selectedArea, getBlockByText(sender, args[1]), IBlockStateUtils.airIBlockState) }
                thread.start()

            }
            else -> modMessage("Usages: Edit, Gui, Load, Fill, Clear, Undo")
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
            1 -> {
                mutableListOf("Edit", "Gui", "Load", "Fill", "Clear", "Undo")
            }
            2 -> {
                getListOfStringsMatchingLastWord(
                    args,
                    Block.blockRegistry.getKeys()
                )
            }
            else -> null
        }
    }

}