package com.github.wadey3636.noobroutes.commands

import me.modcore.Core.display
import me.modcore.ui.clickgui.ClickGUI
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import org.apache.logging.log4j.LogManager

class NoobRoutesCommand : CommandBase() {
    override fun getCommandName(): String {
        return "noobroutes"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "Opens NoobRoutes GUI"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        LogManager.getLogger("Noob Routes").info("Attempting GUI Open!")
        display = ClickGUI
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }
    override fun getCommandAliases(): List<String> {
        return listOf("nr")
    }

}