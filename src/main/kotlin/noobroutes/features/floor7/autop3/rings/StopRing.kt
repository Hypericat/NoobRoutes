package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.features.floor7.autop3.CommandGenerated
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingBase
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.skyblock.PlayerUtils

class StopRing(
    ringBase: RingBase = RingBase(),
) : Ring(ringBase, RingType.STOP) {
    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {
            return StopRing(generateRingBaseFromArgs(args))
        }
    }

    override fun doRing() {
        super.doRing()
        PlayerUtils.stopVelocity()
    }
}