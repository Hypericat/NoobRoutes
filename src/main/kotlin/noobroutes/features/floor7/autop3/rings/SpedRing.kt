package noobroutes.features.floor7.autop3.rings

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.features.floor7.autop3.AutoP3.spedFor
import noobroutes.features.floor7.autop3.AutoP3.timerSpeed
import noobroutes.features.floor7.autop3.Blink
import noobroutes.features.floor7.autop3.Ring
import noobroutes.utils.AutoP3Utils
import noobroutes.utils.skyblock.modMessage

class SpedRing(
    coords: Vec3,
    yaw: Float,
    term: Boolean,
    leap: Boolean,
    left: Boolean,
    center: Boolean,
    rotate: Boolean,
    val length: Int
) : Ring(listOf("speed", "sped"), coords, yaw, term, leap, left, center, rotate) {

    override fun addRingData(obj: JsonObject) {
       obj.addProperty("length", length)
    }

    override fun doRing() {
        super.doRing()
        if (length > Blink.cancelled || spedFor > 0) return
        if (length < 1.0) {
            modMessage("Broken Speed Ring, cancelling execution")
            return

        }

        modMessage("speeding (solid trip)")
        AutoP3Utils.setGameSpeed(timerSpeed)
        spedFor = length
        modMessage(spedFor)
    }
}