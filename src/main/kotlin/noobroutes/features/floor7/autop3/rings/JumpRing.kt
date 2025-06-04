package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.AutoP3Utils
@RingType("Jump")
class JumpRing (
    coords: Vec3 = Vec3(0.0, 0.0, 0.0),
    yaw: Float = 0f,
    term: Boolean = false,
    leap: Boolean = false,
    left: Boolean = false,
    center: Boolean = false,
    rotate: Boolean = false,
    var walk: Boolean = false
) : Ring(coords, yaw, term, leap, left, center, rotate) {


    init {
        addBoolean("walk", {walk}, {walk = it})
    }

    override fun doRing() {
        AutoP3Utils.unPressKeys()
        super.doRing()
        if (mc.thePlayer.onGround) mc.thePlayer.jump()
        if (walk) {
            AutoP3Utils.startWalk(yaw)
            AutoP3Utils.jumping = true
        }
    }
}