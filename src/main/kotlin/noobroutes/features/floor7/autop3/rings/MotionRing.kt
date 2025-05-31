package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.AutoP3Utils

@RingType("Motion")
class MotionRing(
    coords: Vec3 = Vec3(0.0, 0.0, 0.0),
    yaw: Float = 0f,
    term: Boolean = false,
    leap: Boolean = false,
    left: Boolean = false,
    center: Boolean = false,
    rotate: Boolean = false,
) : Ring(coords, yaw, term, leap, left, center, rotate) {
    override fun doRing() {
        AutoP3Utils.unPressKeys()
        super.doRing()
        AutoP3Utils.direction = yaw
        AutoP3Utils.motionTicks = if (AutoP3.fasterMotion && !center) 1 else 0
        AutoP3Utils.motioning = true
    }
}