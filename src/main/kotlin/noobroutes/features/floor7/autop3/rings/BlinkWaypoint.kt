package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingBase
import noobroutes.features.floor7.autop3.RingType


class BlinkWaypoint(
    ringBase: RingBase = RingBase(Vec3(0.0, 0.0, 0.0), 0f, false, false, false, false, false, 1f, 1f),
    val length: Int = 0
) : Ring(ringBase, RingType.BLINK_WAYPOINT) {
    override fun doRing() {

    }
}