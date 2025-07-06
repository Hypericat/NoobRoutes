package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.AutoP3Utils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage

@RingType("Jump")
class JumpRing (
    coords: Vec3 = Vec3(0.0, 0.0, 0.0),
    yaw: Float = 0f,
    term: Boolean = false,
    leap: Boolean = false,
    left: Boolean = false,
    center: Boolean = false,
    rotate: Boolean = false,
    diameter: Float = 1f,
    height: Float = 1f,
    var walk: Boolean = false
) : Ring(coords, yaw, term, leap, left, center, rotate, diameter, height) {


    init {
        addBoolean("walk", {walk}, {walk = it})
    }

    override fun doRing() {
        AutoP3Utils.unPressKeys()
        super.doRing()
        if (AutoP3.renderStyle == 3) modMessage("Jumping", "§0[§6Yharim§0]§7 ")
        if (mc.thePlayer.onGround) mc.thePlayer.jump()
        else return
        if (walk) {
            AutoP3Utils.startWalk(yaw)
            AutoP3Utils.jumping = true
        }
    }

    override fun ringCheckY(): Boolean {
        return coords.yCoord == mc.thePlayer.posY && mc.thePlayer.onGround
    }
}