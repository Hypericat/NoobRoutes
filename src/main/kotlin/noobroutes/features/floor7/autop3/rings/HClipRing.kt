package noobroutes.features.floor7.autop3.rings

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.AutoP3Utils
import noobroutes.utils.skyblock.modMessage
@RingType("HClip")
class HClipRing(
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
        if (walk) AutoP3Utils.walkAfter = true
    }
}