package noobroutes.commands

import net.minecraft.client.Minecraft
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.MovingObjectPosition
import noobroutes.utils.skyblock.modMessage
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection


class LookCommand: CommandBase() {
    override fun getCommandName(): String {
        return "look"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "Sets the rotation of the player"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        if (args == null || args.isEmpty()) return error();
        val yaw: Float = args[0].toFloatOrNull() ?: return error();

        if (args.size == 1) {
            Minecraft.getMinecraft().thePlayer.rotationYaw = yaw;
            return
        }

        val pitch: Float = args[1].toFloatOrNull() ?: return error();
        Minecraft.getMinecraft().thePlayer.rotationYaw = yaw
        Minecraft.getMinecraft().thePlayer.rotationPitch = pitch;
    }

    private fun error() {
        modMessage("Usages: {Float: Yaw}, {Float: Yaw, Float: Pitch}");
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }

    override fun getCommandAliases(): List<String> {
        return listOf("rotate")
    }
}