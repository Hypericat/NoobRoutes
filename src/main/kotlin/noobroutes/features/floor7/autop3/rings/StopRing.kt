package noobroutes.features.floor7.autop3.rings

import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingBase
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.skyblock.PlayerUtils

class StopRing(
    ringBase: RingBase,
) : Ring(ringBase, RingType.STOP) {

    override fun doRing() {
        super.doRing()
        PlayerUtils.stopVelocity()
    }
}