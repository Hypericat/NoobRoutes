package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.AutoP3Utils
@RingType("Stop")
class StopRing(
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
        mc.thePlayer.setVelocity(0.0, mc.thePlayer.motionY, 0.0)
    }
}