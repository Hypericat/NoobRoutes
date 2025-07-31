package noobroutes.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos
import noobroutes.features.misc.SexAura
import noobroutes.utils.Utils
import noobroutes.utils.skyblock.modMessage

class NoobCommand: CommandBase() {
    override fun getCommandName(): String {
        return "noob"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "used to add rings"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        if (args == null || args.isEmpty()) return modMessage("Usages: Rat, Pickup, Test")
        when (args[0].lowercase()) {
            "rat" -> Utils.rat.forEach { modMessage(it) }
            "pickup" -> SexAura.pickupLineByName(args)
            "test" -> Utils.testFunctions(args)
            else -> modMessage("Usages: Rat, Pickup, Test")
        }

    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }


    override fun addTabCompletionOptions(
        sender: ICommandSender,
        args: Array<String>,
        pos: BlockPos
    ): List<String?>? {

        return when (args.size) {
            1 -> getListOfStringsMatchingLastWord(args, listOf( "rat", "pickup", "test"))
            else -> null
        }
    }
}