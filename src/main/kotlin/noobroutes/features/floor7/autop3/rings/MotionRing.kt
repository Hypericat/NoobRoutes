package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.AutoP3Utils
import noobroutes.utils.skyblock.modMessage

@RingType("Motion")
class MotionRing(
    coords: Vec3 = Vec3(0.0, 0.0, 0.0),
    yaw: Float = 0f,
    term: Boolean = false,
    leap: Boolean = false,
    left: Boolean = false,
    center: Boolean = false,
    rotate: Boolean = false,
    diameter: Float = 1f,
    height: Float = 1f,
    var far: Boolean = false,
    var scale: Float = 1f
) : Ring(coords, yaw, term, leap, left, center, rotate, diameter, height) {

    init {
        addBoolean("far", {far}, {far = it})
        addFloat("scale", {scale}, {scale = it})
    }

    override fun doRing() {
        AutoP3Utils.unPressKeys()
        super.doRing()
        if (AutoP3.renderStyle == "cgy") modMessage("Jumping", "§0[§6Yharim§0]§7 ")
        AutoP3Utils.direction = yaw
        mc.thePlayer.motionX = 0.0
        mc.thePlayer.motionZ = 0.0
        AutoP3Utils.motionTicks = 1
        AutoP3Utils.scale = scale
        AutoP3Utils.motioning = true
    }

    override fun ringCheckY(): Boolean {
        return coords.yCoord == mc.thePlayer.posY && mc.thePlayer.onGround
    }
}