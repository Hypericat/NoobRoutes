package com.github.wadey3636.noobroutes.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender

class EtherwarpTestCommand : CommandBase() {
    override fun getCommandName(): String? {
        return "addtestetherwarp"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "test"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {

    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }
    override fun getCommandAliases(): List<String> {
        return listOf()
    }


}