package noobroutes.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import noobroutes.Core.mc
import noobroutes.features.render.ClickGUIModule.devMode
import noobroutes.utils.pathfinding.PathFinder
import noobroutes.utils.pathfinding.PathfinderExecutor
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage

class PathCommand: CommandBase() {
    override fun getCommandName(): String {
        return "path"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "Pathfinding test command"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        if (!devMode) {
            modMessage("Requires Dev Mode")
            return
        }

        if (args == null || args.size < 3) {
            devMessage("invalid args")
        }
        val x = args?.get(0)?.toFloatOrNull() ?: 0f
        val y = args?.get(1)?.toFloatOrNull() ?: 0f
        val z = args?.get(2)?.toFloatOrNull() ?: 0f

        PathfinderExecutor.test(x, y, z)
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }
    override fun getCommandAliases(): List<String> {
        return listOf()
    }
}