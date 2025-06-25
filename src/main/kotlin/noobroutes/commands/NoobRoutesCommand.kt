package noobroutes.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import noobroutes.Core.display
import noobroutes.features.dungeon.Brush
import noobroutes.ui.clickgui.ClickGUI
import noobroutes.utils.skyblock.modMessage
import org.apache.logging.log4j.LogManager

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
        when (args[0]) {
            "brush", "b" -> {
                if (args.size > 1) {
                    when (args[1]) {
                        "e", "edit" -> {
                            Brush.editMode = !Brush.editMode
                            if (!Brush.editMode) {
                                Brush.saveConfig()
                            }
                            modMessage("Toggled Edit Mode ${if (Brush.editMode) "§l§aOn" else "§l§cOff"}")
                        }
                        "g", "gui" -> {

                        }
                        "b", "block" -> {

                        }
                        "load", "l" -> {
                            Brush.loadConfig()
                        }



                    }
                }
            }
            else  -> {
                display = ClickGUI
            }

        }


    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }

    override fun getCommandAliases(): List<String> {
        return listOf("nr")
    }

}