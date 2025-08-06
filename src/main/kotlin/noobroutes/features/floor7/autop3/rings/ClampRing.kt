package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.AutoP3Utils
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart
import noobroutes.utils.skyblock.modMessage

@RingType("Clamp")
class ClampRing(
    coords: Vec3 = Vec3(0.0, 0.0, 0.0),
    yaw: Float = 0f,
    term: Boolean = false,
    leap: Boolean = false,
    left: Boolean = false,
    center: Boolean = false,
    rotate: Boolean = false,
    diameter: Float = 1f,
    height: Float = 1f,
    var walk: Boolean = false
) : Ring(coords, yaw, term, leap, left, center, rotate, diameter, height) {

    init {
        addBoolean("walk", {walk}, {walk = it})
    }

    override fun doRing() {
        AutoP3Utils.unPressKeys()
        super.doRing()
        if (AutoP3.renderStyle == "cgy") modMessage("Looking", "§0[§6Yharim§0]§7 ")
        if (walk) AutoP3Utils.startWalk(yaw)
        val motionX = mc.thePlayer.motionX
        val motionZ = mc.thePlayer.motionZ
        if (motionX * xPart(yaw) < 0 || motionZ * zPart(yaw) < 0) {
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
            return
        }
        val scaleX = if (xPart(yaw) != 0.0) motionX / xPart(yaw) else Double.POSITIVE_INFINITY
        val scaleZ = if (zPart(yaw) != 0.0) motionZ / zPart(yaw) else Double.POSITIVE_INFINITY
        val scale = minOf(scaleX, scaleZ)
        mc.thePlayer.motionX = xPart(yaw) * scale
        mc.thePlayer.motionZ = zPart(yaw) * scale
    }
}