package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingBase
import noobroutes.features.floor7.autop3.RingType


class BlinkWaypoint(
    ringBase: RingBase = RingBase(),
    val length: Int = 0
) : Ring(ringBase, RingType.BLINK_WAYPOINT) {
    override fun doRing() {

    }

    override fun inRing(pos: Vec3): Boolean {
        return checkInBoundsWithSpecifiedHeight(pos,1f) && mc.thePlayer.onGround
    }
}