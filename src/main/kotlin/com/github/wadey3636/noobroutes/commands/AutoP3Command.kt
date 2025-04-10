package com.github.wadey3636.noobroutes.commands

import com.github.wadey3636.noobroutes.features.floor7.AutoP3
import me.defnotstolen.utils.skyblock.modMessage
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender

class AutoP3Command: CommandBase() {
    override fun getCommandName(): String {
        return "noob"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "used to add rings"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        if (args == null || args.isEmpty()) return modMessage("Usages: Add, Delete, Blink, Start")
        AutoP3.handleNoobCommand(args)
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }
    override fun getCommandAliases(): List<String> {
        return listOf("noobp3")
    }
}