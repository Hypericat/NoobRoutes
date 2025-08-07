package noobroutes.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import noobroutes.Core.mc
import noobroutes.features.render.ClickGUIModule.devMode
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage

/**
 * this should probably just be in the noobroutes command
 */
class YawPitchCommand: CommandBase() {
    override fun getCommandName(): String {
        return "setlook"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "for testing"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        if (!devMode) {
            modMessage("Requires Dev Mode")
            return
        }

        if (args == null || args.size < 2) {
            devMessage("invalid args")
        }
        val yaw = args?.get(0)?.toFloatOrNull() ?: 0f
        val pitch = args?.get(1)?.toFloatOrNull() ?: 0f
        mc.thePlayer.rotationYaw = yaw
        mc.thePlayer.rotationPitch = pitch




    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }
    override fun getCommandAliases(): List<String> {
        return listOf()
    }
}