package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.Ring
import noobroutes.utils.AutoP3Utils

class JumpRing (
    coords: Vec3,
    yaw: Float,
    term: Boolean,
    leap: Boolean,
    left: Boolean,
    center: Boolean,
    rotate: Boolean,
    val walk: Boolean
) : Ring("Jump", coords, yaw, term, leap, left, center, rotate) {

    override fun doRing() {
        AutoP3Utils.unPressKeys()
        super.doRing()
        if (mc.thePlayer.onGround) mc.thePlayer.jump()
        if (walk) AutoP3Utils.startWalk(yaw)
    }
}