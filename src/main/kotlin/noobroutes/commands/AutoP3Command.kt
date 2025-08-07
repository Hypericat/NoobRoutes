package noobroutes.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos
import noobroutes.events.BossEventDispatcher
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.CommandGenerated
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.requirement
import noobroutes.utils.skyblock.modMessage
import kotlin.reflect.full.companionObjectInstance

class AutoP3Command: CommandBase() {
    override fun getCommandName(): String {
        return "noob"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "used to add rings"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        if (args == null || args.isEmpty()) return modMessage("Usages: Add, Edit, Delete, clearWaypoints, Start, Load, Undo, Redo")
        when (args[0].lowercase()) {
            "add" -> {
                if (!args.requirement(2)) return modMessage("Rings: ${getCommandGeneratedRings()}")
                val ringType = RingType.getTypeFromName(args[1])
                    ?: return modMessage("Rings: ${getCommandGeneratedRings()}")
                val ring = (ringType.ringClass.companionObjectInstance as? CommandGenerated)?.generateRing(args) ?: return
                AutoP3.addRing(ring)
            }
            "edit" -> {
                AutoP3.handleEdit(args)
            }
            "delete" -> {
                AutoP3.handleDelete(args)
            }
            "redo" -> {
                AutoP3.redo()
            }
            "undo" -> {
                AutoP3.undo()
            }
            "start" -> {
                BossEventDispatcher.inF7Boss = true
                modMessage("started boss")
            }
            "end" -> {
                BossEventDispatcher.inF7Boss = false
                modMessage("ended boss")
            }
            "clearwaypoints" -> {
                AutoP3.setActiveBlinkWaypoint(null)
                modMessage("cleared the waypoint")
            }
            "load" -> {
                AutoP3.loadRings()
                modMessage("loaded rings")
            }
            else -> modMessage("Usages: Add, Edit, Delete, clearWaypoints, Start, Load, Undo, Redo")
        }
    }

    fun getCommandGeneratedRings(): List<String> {
        return RingType.entries.filter { it.commandGenerated }.map { it.ringName }
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }

    override fun getCommandAliases(): List<String?>? {
        return listOf("ap3", "sigma69420autop3gamertime", "autop3", "autophase3", "autophasethree")
    }

    override fun addTabCompletionOptions(
        sender: ICommandSender,
        args: Array<out String?>,
        pos: BlockPos
    ): List<String?>? {
        return when (args.size) {
            0 -> getListOfStringsMatchingLastWord(args, listOf("ap3", "p3", "sigma69420autop3gamertime"))
            1 -> getListOfStringsMatchingLastWord(args, listOf("add", "delete", "clearwaypoints", "start", "restore", "load", "edit"))
            2 -> when (args[0]) { //I hate the amount of nesting here
                "add" -> getListOfStringsMatchingLastWord(args, RingType.entries.filter { it.commandGenerated }.map { it.ringName })
                else -> null
            }
            else -> null
        }
    }
}