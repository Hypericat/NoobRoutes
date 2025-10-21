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
        if (!BrushModule.enabled) return modMessage("Brush module must be enabled first!");
        if (args == null || args.isEmpty()) return modMessage("Usages: Edit, Gui, Clear, Load, Reload, Clear, Undo")
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

            "reload", "r" -> {
                BrushModule.reload()
                modMessage("Reloading")
            }

            "clear", -> {
                BrushModule.clear()
                modMessage("Cleared room / floor config!")
            }

            "undo", "u" -> {
                BrushBuildTools.handleUndo()
            }
            else -> modMessage("Usages: Edit, Gui, Clear, Load, Reload, Fill, Clear, Undo")
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
                mutableListOf("Edit", "Gui", "Clear", "Load", "Reload", "Fill", "Clear", "Undo")
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