package noobroutes.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import noobroutes.Core.display
import noobroutes.ui.clickgui.ClickGui
import noobroutes.ui.test.StencilTest


class NoobRoutesCommand : CommandBase() {
    override fun getCommandName(): String {
        return "noobroutes"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "Opens NoobRoutes GUI"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        if (args != null && args.isNotEmpty()) {
            display = StencilTest
            return
        }
        display = ClickGui
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }

    override fun getCommandAliases(): List<String> {
        return listOf("nr")
    }


}