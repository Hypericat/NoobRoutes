package noobroutes.features.floor7.autop3.rings

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType
import noobroutes.features.move.LavaClip
import noobroutes.utils.AutoP3Utils
@RingType("LavaClip")
class LavaClipRing(
    coords: Vec3,
    yaw: Float,
    term: Boolean,
    leap: Boolean,
    left: Boolean,
    center: Boolean,
    rotate: Boolean,
    val length: Double
) : Ring(coords, yaw, term, leap, left, center, rotate) {

    override fun addRingData(obj: JsonObject) {
        obj.addProperty("length", length)
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