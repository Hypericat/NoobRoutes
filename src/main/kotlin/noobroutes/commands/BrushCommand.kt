package noobroutes.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import noobroutes.Core.logger
import noobroutes.features.dungeon.brush.Brush
import noobroutes.features.dungeon.brush.FMEBlockDeserializer
import noobroutes.features.dungeon.brush.FmeConfigLoader

import noobroutes.utils.skyblock.modMessage

class BrushCommand: CommandBase() {
    override fun getCommandName(): String {
        return "brush"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "to utilize the brush feature"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        if (args == null || args.isEmpty()) return modMessage("Usages: Add, Delete, Set, Clear, Load")
        when (args[0]) {
            "em", "e", "edit" -> {
                toggleEditMode()
            }
            "g", "gui" -> {

            }
            "b", "block" -> {

            }
            "load", "l" -> {
                Brush.loadConfig()
            }
            "fml" -> {
                FmeConfigLoader.loadConfig()
                logger.info("config:${FmeConfigLoader.config}")
                logger.info("floorConfig:${FmeConfigLoader.floorsConfig}")
            }
        }
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }
    override fun getCommandAliases(): List<String> {
        return listOf("br")
    }
    private fun toggleEditMode(){
        Brush.editMode = !Brush.editMode
        if (!Brush.editMode) {
            Brush.saveConfig()
        }
        modMessage("Toggled Edit Mode ${if (Brush.editMode) "§l§aOn" else "§l§cOff"}")
    }

}