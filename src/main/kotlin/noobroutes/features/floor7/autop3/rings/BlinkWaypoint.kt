package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.features.floor7.autop3.Ring


class BlinkWaypoint(
    coords: Vec3 = Vec3(0.0, 0.0, 0.0),
    yaw: Float = 0f,
    term: Boolean = false,
    leap: Boolean = false,
    left: Boolean = false,
    center: Boolean = false,
    rotate: Boolean = false,
    val length: Int = 0,
    val walk: Boolean = false
) : Ring(coords, yaw, term, leap, left, center, rotate) {

    override fun doRing() {

    }
}