package noobroutes.features.floor7.autop3.rings

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.Ring
import noobroutes.utils.AutoP3Utils
import noobroutes.utils.skyblock.modMessage

class HClipRing(
    coords: Vec3,
    yaw: Float,
    term: Boolean,
    leap: Boolean,
    left: Boolean,
    center: Boolean,
    rotate: Boolean,
    val walk: Boolean
) : Ring(listOf("hclip"), coords, yaw, term, leap, left, center, rotate) {

    override fun addRingData(obj: JsonObject) {
        if (walk) obj.addProperty("walk", true)
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