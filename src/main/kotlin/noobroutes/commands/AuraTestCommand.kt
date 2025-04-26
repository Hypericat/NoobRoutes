package com.github.wadey3636.noobroutes.commands

import com.github.wadey3636.noobroutes.Core.mc
import com.github.wadey3636.noobroutes.features.test.AuraTest.blockAuraList
import com.github.wadey3636.noobroutes.features.render.ClickGUIModule.devMode
import com.github.wadey3636.noobroutes.utils.skyblock.modMessage
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender

class AuraTestCommand: CommandBase() {
    override fun getCommandName(): String {
        return "addAuraTest"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "used to add rings"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        if (!devMode) {
            modMessage("Requires Dev Mode")
            return
        }
        blockAuraList.add(mc.objectMouseOver.blockPos)



    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }
    override fun getCommandAliases(): List<String> {
        return listOf()
    }
}