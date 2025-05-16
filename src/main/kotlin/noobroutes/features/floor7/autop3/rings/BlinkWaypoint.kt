package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType

@RingType("Name")
class BlinkWaypoint(
    coords: Vec3,
    yaw: Float,
    term: Boolean,
    leap: Boolean,
    left: Boolean,
    center: Boolean,
    rotate: Boolean,
    val length: Int
) : Ring(coords, yaw, term, leap, left, center, rotate) {

    override fun doRing() {

    }
}