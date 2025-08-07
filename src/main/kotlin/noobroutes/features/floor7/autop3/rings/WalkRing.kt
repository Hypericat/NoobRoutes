package noobroutes.features.floor7.autop3.rings

import noobroutes.features.floor7.autop3.*


class WalkRing(
    ringBase: RingBase = RingBase(),
) : Ring(ringBase, RingType.WALK) {
    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {
            return if (args.any {it == "jump"}) JumpRing(generateRingBaseFromArgs(args), true) else WalkRing(generateRingBaseFromArgs(args))
        }
    }

    override fun doRing() {
        super.doRing()

        AutoP3MovementHandler.setDirection(yaw)
    }
}