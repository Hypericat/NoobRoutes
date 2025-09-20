package noobroutes.features.floor7.autop3.rings

import noobroutes.features.floor7.autop3.*
import noobroutes.ui.editgui.EditGuiBase
import noobroutes.utils.requirement
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.sendCommand

class CommandRing(ringBase: RingBase = RingBase(), var walk: Boolean = false, var command: String = "") : Ring(ringBase, RingType.COMMAND) {
    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {
            if (!requirement(3, args)) {
                modMessage("Requires a command arg")
                return null
            }
            val indexOfFirstArg = args.indexOfFirst { isArg(it) }
            val indexOfLast = when (indexOfFirstArg) {
                -1 -> {
                    val argsLength = args.size - 1
                    if (argsLength == 2) -1 else argsLength
                }
                else -> {
                    val argsLength = indexOfFirstArg - 1
                    if (argsLength == 2) -1 else argsLength
                }
            }
            var commandStart = args[2].removePrefix("/")
            val command = if (indexOfLast == -1) {
                commandStart
            } else {
                for (i in 3..indexOfLast) {
                    commandStart += " ${args[i]}"
                }
                commandStart
            }
            val walk = getWalkFromArgs(args)

            return CommandRing(generateRingBaseFromArgs(args), walk, command)
        }
        fun isArg(text: String): Boolean{
            return when (text) {
                "leap" -> true
                "term" -> true
                "left" -> true
                "center" -> true
                "rotate" -> true
                "look" -> true
                "walk" -> true
                else -> false
            }
        }
    }

    init {
        addString("command", {command}, {command = it})
        addBoolean("walk", {walk}, {walk = it})
    }

    override fun doRing() {
        if (hasArgs()) super.doRing()
        if (walk) AutoP3MovementHandler.setDirection(yaw)

        sendCommand(command, false)
    }

    private fun hasArgs(): Boolean{
        return await != RingAwait.NONE || center || rotate || walk
    }
    override fun extraArgs(builder: EditGuiBase.EditGuiBaseBuilder) {
        builder.addSwitch("Walk", {walk}, {walk = it})
    }
}