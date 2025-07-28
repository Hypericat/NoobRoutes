package noobroutes.features.floor7.autop3.rings

import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.AutoP3MovementHandler
import noobroutes.features.floor7.autop3.CommandGenerated
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingBase
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart


class ClampRing(
    ringBase: RingBase,
    var walk: Boolean = false
) : Ring(ringBase, RingType.CLAMP) {
    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {
            return ClampRing(generateRingBaseFromArgs(args), getWalkFromArgs(args))
        }
    }

    init {
        addBoolean("walk", {walk}, {walk = it})
    }

    override fun doRing() {
        super.doRing()

        if (walk) AutoP3MovementHandler.setDirection(yaw)

        val motionX = mc.thePlayer.motionX
        val motionZ = mc.thePlayer.motionZ
        if (motionX * xPart(yaw) < 0 || motionZ * zPart(yaw) < 0) {
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
            return
        }
        val scaleX = if (xPart(yaw) != 0.0) motionX / xPart(yaw) else Double.POSITIVE_INFINITY
        val scaleZ = if (zPart(yaw) != 0.0) motionZ / zPart(yaw) else Double.POSITIVE_INFINITY
        val scale = minOf(scaleX, scaleZ)
        mc.thePlayer.motionX = xPart(yaw) * scale
        mc.thePlayer.motionZ = zPart(yaw) * scale
    }
}