package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.BlockPos
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.CommandGenerated
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingBase
import noobroutes.features.floor7.autop3.RingType
import noobroutes.features.misc.TimerHud
import noobroutes.utils.AuraManager
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.isAir
import noobroutes.utils.skyblock.modMessage


class BoomRing(
    ringBase: RingBase = RingBase(),
    var block: BlockPos = BlockPos(0, 0, 0),
) : Ring(ringBase, RingType.BOOM) {
    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {
            val block = mc.objectMouseOver.blockPos
            if (isAir(block)) {
                modMessage("must look at a block")
                return null
            }
            return BoomRing(generateRingBaseFromArgs(args), block)
        }
    }


    init {
        addBlockPos("block", {block}, {block = it})
    }

    override fun doRing() {
        val swapped = SwapManager.swapFromName("TNT")
        if (stopWatch) TimerHud.toggle()
        if (swapped == SwapManager.SwapState.ALREADY_HELD) {
            AuraManager.auraBlock(block, force = true)
            return
        }
        Scheduler.schedulePreTickTask(1) { AuraManager.auraBlock(block, force = true) }
    }
}