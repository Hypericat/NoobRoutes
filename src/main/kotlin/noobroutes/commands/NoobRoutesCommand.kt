package noobroutes.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos
import noobroutes.Core.display
import noobroutes.features.dungeon.AutoBloodRush
import noobroutes.features.misc.SexAura
import noobroutes.ui.clickgui.ClickGUI
import noobroutes.utils.Utils
import noobroutes.utils.skyblock.dungeon.Dungeon
import noobroutes.utils.skyblock.modMessage

class NoobRoutesCommand : CommandBase() {
    override fun getCommandName(): String {
        return "noobroutes"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "Opens NoobRoutes GUI"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        if (args == null || args.isEmpty()) {
            display = ClickGUI
            return
        }

        when (args[0].lowercase()) {
            "rat" -> Utils.rat.forEach { modMessage(it) }
            "pickup" -> SexAura.pickupLineByName(args)
            "test" -> Utils.testFunctions(args)
            "snipe" -> AutoBloodRush.snipeCommand(args)
            else -> modMessage("Usages: Rat, Pickup, Snipe, Test")
        }
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }

    override fun getCommandAliases(): List<String> {
        return listOf("nr")
    }

    override fun addTabCompletionOptions(
        sender: ICommandSender,
        args: Array<String>,
        pos: BlockPos
    ): List<String?>? {

        return when (args.size) {
            1 -> getListOfStringsMatchingLastWord(args, listOf( "rat", "pickup", "test", "snipe"))
            2 -> if (args[0] == "snipe") getListOfStringsMatchingLastWord(args, Dungeon.Info.uniqueRooms.map { it.name.replace(" ", "_") } + "Boss") else null
            else -> null
        }
    }

}