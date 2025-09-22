package noobroutes.commands

import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos
import noobroutes.events.BossEventDispatcher
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.CommandGenerated
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.getArg
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
        if (args == null || args.isEmpty()) return modMessage("Usages: Add, Edit, Delete, clearWaypoints, Start, Load, Undo, Redo, em")
        when (args[0].lowercase()) {
            "add", "erect" -> {
                val ringName = args.getArg(1, "Rings: ${getCommandGeneratedRings()}") ?: return
                val ringType = RingType.getTypeFromName(ringName)
                    ?: return modMessage("Rings: ${getCommandGeneratedRings()}")
                val ring = (ringType.ringClass.companionObjectInstance as? CommandGenerated)?.generateRing(args) ?: return
                AutoP3.addRing(ring)
            }
            "editmode", "em" -> {
                AutoP3.toggleEditMode()
            }

            "edit" -> {
                AutoP3.handleEdit(args)
            }
            "delete", "remove" -> {
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
            "loadfile" -> {
                AutoP3.loadRings()
                modMessage("loaded rings")
            }

            "load" -> {
                val route = args.getArg(1, "Specify the config to load!") ?: return
                AutoP3.route = route
                modMessage("loading config $route")
            }

            else -> modMessage("Usages: Add, Edit, Delete, clearWaypoints, Start, LoadFile, Undo, Redo, Load")
        }
    }

    fun getCommandGeneratedRings(): List<String> {
        return RingType.entries.filter { it.commandGenerated }.map { it.ringName }
    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }

    override fun getCommandAliases(): List<String?>? {
        return listOf("ap3", "sigma69420autop3gamertime", "autop3", "autophase3", "autophasethree", "n", "nb")
    }

    override fun addTabCompletionOptions(
        sender: ICommandSender,
        args: Array<out String?>,
        pos: BlockPos
    ): List<String?>? {
        return when (args.size) {
            0 -> getListOfStringsMatchingLastWord(args, listOf("ap3", "p3", "sigma69420autop3gamertime", "n"))
            1 -> getListOfStringsMatchingLastWord(args, listOf("add", "delete", "clearwaypoints", "start", "restore", "load", "edit"))
            2 -> when (args[0]) { //I hate the amount of nesting here
                "add" -> getListOfStringsMatchingLastWord(args, RingType.entries.filter { it.commandGenerated }.map { it.ringName })
                else -> null
            }
            else -> null
        }
    }
}