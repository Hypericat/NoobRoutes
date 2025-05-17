package noobroutes.features.floor7.autop3.rings

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.features.floor7.autop3.AutoP3.spedFor
import noobroutes.features.floor7.autop3.AutoP3.timerSpeed
import noobroutes.features.floor7.autop3.Blink
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.AutoP3Utils
import noobroutes.utils.skyblock.modMessage
@RingType("Speed")
class SpedRing(
    coords: Vec3 = Vec3(0.0, 0.0, 0.0),
    yaw: Float = 0f,
    term: Boolean = false,
    leap: Boolean = false,
    left: Boolean = false,
    center: Boolean = false,
    rotate: Boolean = false,
    var length: Int = 0
) : Ring(coords, yaw, term, leap, left, center, rotate) {


    init {
        addInt("length", {length}, {length = it})
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