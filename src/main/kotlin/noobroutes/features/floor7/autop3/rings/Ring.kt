package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.AutoP3.fuckingLook
import noobroutes.features.floor7.autop3.AutoP3.silentLook
import noobroutes.features.floor7.autop3.Blink

abstract class Ring(
    val coords: Vec3,
    val yaw: Float,
    val term: Boolean,
    val leap: Boolean,
    val left: Boolean,
    val center: Boolean,
    val rotate: Boolean
) {
    open fun doRing() { //fuck u wadey
        if (center && !mc.thePlayer.onGround) return //add shit so it does when on ground
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
}