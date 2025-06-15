package noobroutes.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import noobroutes.features.misc.EWPathfinderModule
import noobroutes.features.render.ClickGUIModule.devMode
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage

class PathCommand: CommandBase() {
    override fun getCommandName(): String {
        return "path"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "Pathfinding command"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        if (args == null || args.size < 3) {
            devMessage("Invalid args!")
            return
        }

        val x = args[0].toFloatOrNull() ?: 0f
        val y = args[1].toFloatOrNull() ?: 0f
        val z = args[2].toFloatOrNull() ?: 0f

        EWPathfinderModule.execute(x, y, z);
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }
    override fun getCommandAliases(): List<String> {
        return listOf()
    }
}