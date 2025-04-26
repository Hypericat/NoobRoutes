package noobroutes.commands

import noobroutes.Core.mc
import noobroutes.features.test.EtherwarpTest
import noobroutes.utils.render.RenderUtils.renderVec
import noobroutes.utils.skyblock.EtherWarpHelper
import noobroutes.utils.skyblock.devMessage
import net.minecraft.command.CommandBase
import net.minecraft.command.ICommandSender

class EtherwarpTestCommand : CommandBase() {
    override fun getCommandName(): String? {
        return "addtestetherwarp"
    }

    override fun getCommandUsage(sender: ICommandSender?): String {
        return "test"
    }

    override fun processCommand(sender: ICommandSender?, args: Array<out String>?) {
        val etherwarp = EtherWarpHelper.getEtherPos()
        if (!etherwarp.succeeded || etherwarp.vec == null) {
            devMessage("Failed to get etherwarp")
            return
        }

        EtherwarpTest.etherwarps.add(Pair(mc.thePlayer.renderVec, etherwarp.vec!!))


    }

    override fun canCommandSenderUseCommand(sender: ICommandSender?): Boolean {
        return true
    }
    override fun getCommandAliases(): List<String> {
        return listOf()
    }


}