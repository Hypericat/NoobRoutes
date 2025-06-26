package noobroutes.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import noobroutes.Core
import noobroutes.Core.logger
import noobroutes.features.dungeon.Brush
import noobroutes.ui.blockgui.BlockGui

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
            else -> modMessage("Usages: Edit, Gui, Load, LoadFME")
        }
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }
    override fun getCommandAliases(): List<String> {
        return listOf("br")
    }


}