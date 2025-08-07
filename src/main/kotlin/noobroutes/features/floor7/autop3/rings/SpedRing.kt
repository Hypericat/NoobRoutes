package noobroutes.features.floor7.autop3.rings

import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingBase
import noobroutes.features.floor7.autop3.RingType


class SpedRing(
    ringBase: RingBase = RingBase(),
    var length: Int = 0
) : Ring(ringBase, RingType.SPEED) {

    init {
        addInt("length", {length}, {length = it})
    }

    override fun doRing() {
        /*if (length > AutoP3.cancelled || AutoP3.blinksThisInstance + length > AutoP3.getMaxBlinks()) return

        if (length < 1.0) {
            modMessage("Broken Speed Ring, cancelling execution")
            return
        }
        AutoP3.blinksThisInstance += length
        AutoP3.cancelled -= length

        repeat(length ) { mc.runTick() }*/
    }


}