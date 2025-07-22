package noobroutes.features.floor7.autop3.rings

import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.Blink
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.AutoP3Utils
import noobroutes.utils.PacketUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage

@RingType("Walk")
class WalkRing(
    coords: Vec3 = Vec3(0.0, 0.0, 0.0),
    yaw: Float = 0f,
    term: Boolean = false,
    leap: Boolean = false,
    left: Boolean = false,
    center: Boolean = false,
    rotate: Boolean = false,
    diameter: Float = 1f,
    height: Float = 1f
) : Ring(coords, yaw, term, leap, left, center, rotate, diameter, height) {

    override fun doRing() {
        AutoP3Utils.unPressKeys()
        super.doRing()
        if (AutoP3.renderStyle == 3) modMessage("Looking", "§0[§6Yharim§0]§7 ")
        if (!center) AutoP3Utils.startWalk(yaw)
        else {
            Scheduler.schedulePostMoveEntityWithHeadingTask(2) { mc.thePlayer.setPosition(coords.xCoord, mc.thePlayer.posY, coords.zCoord) }
            Scheduler.schedulePostTickTask(1) { AutoP3Utils.startWalk(yaw) }
        }
    }
}