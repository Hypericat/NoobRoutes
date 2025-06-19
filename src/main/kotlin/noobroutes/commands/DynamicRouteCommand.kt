package noobroutes.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.routes.DynamicRoute
import noobroutes.utils.skyblock.modMessage

class DynamicRouteCommand: CommandBase() {
    override fun getCommandName(): String {
        return "dynamicroute"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "used to edit autoroutes"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        if (args == null || args.isEmpty()) return modMessage("Usages: Add, Delete, Edit, Clear, Load")
        DynamicRoute.handleDynamicRouteCommand(args)
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }
    override fun getCommandAliases(): List<String> {
        return listOf("dr", "dynroute")
    }

}