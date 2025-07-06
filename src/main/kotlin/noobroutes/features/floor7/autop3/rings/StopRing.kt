package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.AutoP3Utils
import noobroutes.utils.skyblock.modMessage

@RingType("Stop")
class StopRing(
    coords: Vec3 = Vec3(0.0, 0.0, 0.0),
    yaw: Float = 0f,
    term: Boolean = false,
    leap: Boolean = false,
    left: Boolean = false,
    center: Boolean = false,
    rotate: Boolean = false,
    diameter: Float = 1f,
    height: Float = 1f
) : Ring(coords, yaw, term, leap, left, center, rotate, diameter, height) {

    override fun doRing() {
        AutoP3Utils.unPressKeys()
        super.doRing()
        if (AutoP3.renderStyle == 3) modMessage("Stopping", "§0[§6Yharim§0]§7 ")
        mc.thePlayer.setVelocity(0.0, mc.thePlayer.motionY, 0.0)
    }
}