package noobroutes.features.floor7.autop3.rings

import com.google.gson.JsonObject
import noobroutes.features.floor7.autop3.*
import noobroutes.features.floor7.autop3.WalkBoost.Companion.asWalkBoost
import noobroutes.ui.editgui.EditGuiBase
import noobroutes.utils.capitalizeFirst
import noobroutes.utils.requirement
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.sendCommand

class CommandRing(ringBase: RingBase = RingBase(), var walk: Boolean = false, var clientSide: Boolean = false, var command: String = "", var walkBoost: WalkBoost = WalkBoost.UNCHANGED) : Ring(ringBase, RingType.COMMAND) {
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
            val clientSide = args.any {it.lowercase() == "client"}
            val boost = getWalkBoost(args)

            return CommandRing(generateRingBaseFromArgs(args), walk, clientSide, command, boost)
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
                "none" -> true
                "unchanged" -> true
                "normal" -> true
                "large" -> true
                else -> false
            }
        }
    }

    init {
        addString("command", {command}, {command = it})
        addBoolean("walk", {walk}, {walk = it})
        addBoolean("client_side", {clientSide}, {clientSide = it})
    }

    override fun addRingData(obj: JsonObject) {
        obj.addProperty("boost", walkBoost.name)
    }

    override fun loadRingData(obj: JsonObject) {
        super.loadRingData(obj)
        walkBoost = obj.get("boost")?.asWalkBoost() ?: WalkBoost.UNCHANGED
    }


    override fun doRing() {
        if (hasArgs()) super.doRing()
        if (walkBoost != WalkBoost.UNCHANGED) {
            AutoP3.walkBoost = walkBoost.name.lowercase().capitalizeFirst()
        }
        if (walk) AutoP3MovementHandler.setDirection(yaw)

        sendCommand(command, clientSide)
    }

    private fun hasArgs(): Boolean{
        return ringAwaits.isNotEmpty() || center || rotate || walk
    }
    override fun extraArgs(builder: EditGuiBase.EditGuiBaseBuilder) {
        builder.addSwitch("Walk", {walk}, {walk = it})
        builder.addSwitch("Client Side", {clientSide}, {clientSide = it})
        builder.addSelector(
            "Walk Boost",
            WalkBoost.getOptionsList(),
            { walkBoost.getIndex() },
            { walkBoost = WalkBoost[it]}
        )
    }
}