package noobroutes.features.floor7.autop3.rings

import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.AutoP3MovementHandler
import noobroutes.features.floor7.autop3.CommandGenerated
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingBase
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.Scheduler
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage


class MotionRing(
    ringBase: RingBase,
    var far: Boolean = false,
    var scale: Float = 1f
) : Ring(ringBase, RingType.MOTION) {
    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {
           if (args.size < 3) {
               modMessage("need a scale arg (0-1)")
               return null
            }
           val scale = args[2].toFloatOrNull()
           if (scale == null) {
               modMessage("need a scale arg (0-1)")
               return null
           }
            return MotionRing(generateRingBaseFromArgs(args), getWalkFromArgs(args), scale)
        }
    }

    init {
        addBoolean("far", {far}, {far = it})
        addFloat("scale", {scale}, {scale = it})
    }

    override fun doRing() {
        super.doRing()

        if (!mc.thePlayer.onGround) {
            triggered = false
            return
        }
        else mc.thePlayer.jump()

        devMessage("${System.currentTimeMillis()}, motion ring")

        AutoP3MovementHandler.setDirection(yaw)
        AutoP3MovementHandler.setMotionTicks(0)
        AutoP3MovementHandler.setScale(scale)
    }
}