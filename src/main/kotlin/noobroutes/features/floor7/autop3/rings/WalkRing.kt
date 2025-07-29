package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.features.floor7.autop3.AutoP3MovementHandler
import noobroutes.features.floor7.autop3.CommandGenerated
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingBase
import noobroutes.features.floor7.autop3.RingType


class WalkRing(
    ringBase: RingBase = RingBase(Vec3(0.0, 0.0, 0.0), 0f, false, false, false, false, false, 1f, 1f),
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