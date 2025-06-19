package noobroutes.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender

import noobroutes.utils.skyblock.modMessage

class AutoBloodRushCommand: CommandBase() {
    override fun getCommandName(): String {
        return "bloodrush"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "used to edit bloodrush routes"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        if (args == null || args.isEmpty()) return modMessage("Usages: Add, Delete, Set, Clear, Load")
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }
    override fun getCommandAliases(): List<String> {
        return listOf("br")
    }
}