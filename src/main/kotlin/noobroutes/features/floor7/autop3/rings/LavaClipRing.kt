package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType
import noobroutes.features.move.LavaClip
import noobroutes.utils.AutoP3Utils

@RingType("LavaClip")
class LavaClipRing(
    coords: Vec3 = Vec3(0.0, 0.0, 0.0),
    yaw: Float = 0f,
    term: Boolean = false,
    leap: Boolean = false,
    left: Boolean = false,
    center: Boolean = false,
    rotate: Boolean = false,
    var length: Double = 0.0
) : Ring(coords, yaw, term, leap, left, center, rotate) {

    init {
        addDouble("length", {length}, {length = it})
    }


    override fun doRing() {
        AutoP3Utils.unPressKeys()
        super.doRing()
        mc.thePlayer.motionX = 0.0
        mc.thePlayer.motionZ = 0.0
        LavaClip.ringClip = length
        LavaClip.toggle()
    }
}