package noobroutes.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos
import noobroutes.Core.display
import noobroutes.features.ModuleManager
import noobroutes.features.dungeon.AutoBloodRush
import noobroutes.features.misc.SexAura
import noobroutes.features.misc.TickControl
import noobroutes.ui.clickgui.ClickGui
import noobroutes.utils.Utils
import noobroutes.utils.requirement
import noobroutes.utils.skyblock.LowHopUtils
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
            display = ClickGui
            return
        }

        when (args[0].lowercase()) {
            "rat" -> Utils.rat.forEach { modMessage(it) }
            "pickup" -> SexAura.pickupLineByName(args)
            "test" -> Utils.testFunctions(args)
            "snipe" -> AutoBloodRush.snipeCommand(args)
            "tick" -> TickControl.tick(args.getOrNull(1)?.toIntOrNull() ?: return modMessage("Invalid usage!"))
            "toggle" -> {
                if (!requirement(2, args)) {
                    val stringBuilder = StringBuilder()
                    stringBuilder.appendLine("Requires a Module:")

                    for (module in ModuleManager.modules) {
                       stringBuilder.appendLine(module.javaClass.simpleName.lowercase())
                    }
                    modMessage(stringBuilder.toString())
                    return
                }

                for (module in ModuleManager.modules) {
                    if (module.javaClass.simpleName.lowercase() != args[1].lowercase()) continue
                    module.toggle()
                    modMessage("${module.name} ${if (module.enabled) "§aenabled" else "§cdisabled"}.")
                    return
                }
                return modMessage("Invalid Module Name")

            }
            "startdisabler" -> LowHopUtils.disable()
            "makemydisablerjustfuckinggopls" -> {
                modMessage("set disabled to true")
                LowHopUtils.disabled = true
            }

            else -> modMessage("Usages: Rat, Pickup, Snipe, Test, Toggle, StartDisabler")
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
            1 -> getListOfStringsMatchingLastWord(args, listOf( "rat", "pickup", "test", "snipe", "startdisabler"))
            2 -> if (args[0] == "snipe") getListOfStringsMatchingLastWord(args, Dungeon.Info.uniqueRooms.map { it.name.replace(" ", "_") } + "Boss") else null
            else -> null
        }
    }

}