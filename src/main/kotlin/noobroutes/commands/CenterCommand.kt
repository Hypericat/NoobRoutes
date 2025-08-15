package noobroutes.commands

import net.minecraft.client.Minecraft
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.MovingObjectPosition
import noobroutes.CenterType
import noobroutes.utils.skyblock.modMessage
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection


class CenterCommand: CommandBase() {
    override fun getCommandName(): String {
        return "center"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "Centers the player"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        if (args == null) return error();
        if (args.isEmpty()) {
            CenterType.POS.run();
            return
        }

        val type: CenterType = CenterType.fromString(args[0]) ?: return error();
        type.run();
        modMessage("Centered!")
    }

    private fun error() {
        modMessage("Usages: {}, {All, Angles, Pos, X, Y, Z, Yaw, Pitch, Edge, Center}");
    }
    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }

    override fun getCommandAliases(): List<String> {
        return listOf()
    }
}