package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.CommandGenerated
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingBase
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.skyblock.modMessage


class SpedRing(
    ringBase: RingBase = RingBase(),
    var length: Int = 0
) : Ring(ringBase, RingType.SPEED) {
    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {
            return SpedRing(generateRingBaseFromArgs(args), 20)
        }
    }

    init {
        addInt("length", {length}, {length = it})
    }

    override fun doRing() {
        if (length > AutoP3.cancelled || AutoP3.blinksThisInstance + length > AutoP3.getMaxBlinks()) return

        if (length < 1.0) {
            modMessage("Broken Speed Ring, cancelling execution")
            return
        }
        AutoP3.blinksThisInstance += length
        AutoP3.cancelled -= length

        repeat(length ) { mc.runTick() }
    }


}