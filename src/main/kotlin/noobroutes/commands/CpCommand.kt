package noobroutes.commands

import net.minecraft.client.Minecraft
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.MovingObjectPosition
import noobroutes.utils.skyblock.modMessage
import java.awt.Toolkit
import java.awt.datatransfer.Clipboard
import java.awt.datatransfer.StringSelection


class CpCommand: CommandBase() {
    override fun getCommandName(): String {
        return "copy"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "Copies the block positon"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        val obj = Minecraft.getMinecraft().objectMouseOver
        if (obj.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
            modMessage("No block selected!")
            return
        }
        val string = "" + obj.blockPos.x + " " + obj.blockPos.y + " " + obj.blockPos.z
        modMessage("Copied : $string")

        val selection = StringSelection(string)
        val clipboard: Clipboard = Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(selection, selection)
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }
    override fun getCommandAliases(): List<String> {
        return listOf("cp")
    }
}