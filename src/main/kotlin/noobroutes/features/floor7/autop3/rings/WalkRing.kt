package noobroutes.features.floor7.autop3.rings

import noobroutes.features.floor7.autop3.AutoP3MovementHandler
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingBase
import noobroutes.features.floor7.autop3.RingType


class WalkRing(
    ringBase: RingBase,
) : Ring(ringBase, RingType.WALK) {

    override fun generateRingFromArgs(args: Array<out String>): Ring? {
        return if (args.any {it == "jump"}) JumpRing(generateRingBaseFromArgs(args), true) else WalkRing(generateRingBaseFromArgs(args))
    }

    override fun doRing() {
        super.doRing()

        AutoP3MovementHandler.setDirection(yaw)
    }
}