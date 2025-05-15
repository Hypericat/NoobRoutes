package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.features.floor7.autop3.Ring
import noobroutes.utils.AutoP3Utils

class WalkRing(
    coords: Vec3,
    yaw: Float,
    term: Boolean,
    leap: Boolean,
    left: Boolean,
    center: Boolean,
    rotate: Boolean
) : Ring(listOf("walk"), coords, yaw, term, leap, left, center, rotate) {

    override fun doRing() {
        AutoP3Utils.unPressKeys()
        super.doRing()
        AutoP3Utils.startWalk(yaw)
    }
}