package noobroutes.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import noobroutes.Core.mc
import noobroutes.features.render.ClickGUIModule.devMode
import noobroutes.features.test.AuraTest.blockAuraList
import noobroutes.utils.skyblock.modMessage

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