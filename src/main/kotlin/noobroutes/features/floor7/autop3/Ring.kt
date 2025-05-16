package noobroutes.features.floor7.autop3

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.AutoP3.fuckingLook
import noobroutes.features.floor7.autop3.AutoP3.silentLook
import noobroutes.utils.JsonHelper.addProperty


abstract class Ring(
    val name: String,
    val coords: Vec3 = Vec3(mc.thePlayer?.posX ?: 0.0, mc.thePlayer?.posY ?: 0.0, mc.thePlayer?.posZ ?: 0.0),
    val yaw: Float,
    val term: Boolean,
    val leap: Boolean,
    val left: Boolean,
    val center: Boolean,
    val rotate: Boolean
) {
    var triggered = false

    fun getAsJsonObject(): JsonObject{
        val obj = JsonObject().apply {
            addProperty("type", name)
            addProperty("coords", coords)
            addProperty("yaw", yaw)
            if (term) addProperty("term", true)
            if (leap) addProperty("leap", true)
            if (left) addProperty("left", true)
            if (center) addProperty("center", true)
            if (rotate) addProperty("rotate", true)
        }
        addRingData(obj)
        return obj
    }

    open fun addRingData(obj: JsonObject) {}

    open fun doRing() { //fuck u wadey
        if (rotate) {
            if (!silentLook) mc.thePlayer.rotationYaw = yaw
            Blink.rotate = yaw
        }
        if (fuckingLook) {
            mc.thePlayer.rotationYaw = yaw
        }
        if (center) {
            mc.thePlayer.setPosition(coords.xCoord, mc.thePlayer.posY, coords.zCoord)
            Blink.rotSkip = true
        }
    }

    fun run() {
        if (center && !mc.thePlayer.onGround) {
            triggered = false
            return
        }
        doRing()
    }
}