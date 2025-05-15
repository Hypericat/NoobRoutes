package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.utils.AutoP3Utils
import noobroutes.utils.skyblock.modMessage

class HClipRing(
    coords: Vec3,
    yaw: Float,
    term: Boolean,
    leap: Boolean,
    left: Boolean,
    center: Boolean,
    rotate: Boolean
) : Ring(coords, yaw, term, leap, left, center, rotate) {

    override fun doRing() {
        if (mc.thePlayer.onGround) {
            modMessage("use jump or yeet, not hclip")
            return
        }
        AutoP3Utils.unPressKeys()
        super.doRing()
        mc.thePlayer.motionX = 0.0
        mc.thePlayer.motionZ = 0.0
        AutoP3Utils.awaitingTick = true
        AutoP3Utils.direction = yaw
    }
}