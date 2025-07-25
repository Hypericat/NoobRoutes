package noobroutes.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.utils.skyblock.dungeon.Dungeon
import noobroutes.utils.skyblock.modMessage

class AutoP3Command: CommandBase() {
    override fun getCommandName(): String {
        return "noob"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "used to add rings"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        if (args == null || args.isEmpty()) return modMessage("Usages: Add, Delete, Blink, Start, Restore, Load")
        AutoP3.handleNoobCommand(args)
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }
    override fun getCommandAliases(): List<String> {
        return listOf("noobp3")
    }

    override fun addTabCompletionOptions(
        sender: ICommandSender,
        args: Array<String>,
        pos: BlockPos
    ): List<String?>? {
        return when (args.size) {
            1 -> getListOfStringsMatchingLastWord(
                args,
                listOf(
                    "add", "create", "delete", "remove", "blink", "edit",
                    "start", "rat", "pickup", "restore", "test", "load", "snipe"
                )
            )
            2 -> if (args[0] == "snipe") getListOfStringsMatchingLastWord(args, Dungeon.Info.uniqueRooms.map { it.name.replace(" ", "_") }) else null
            else -> null
        }
    }
}