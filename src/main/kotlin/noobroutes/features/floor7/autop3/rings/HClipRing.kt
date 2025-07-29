package noobroutes.features.floor7.autop3.rings

import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.AutoP3MovementHandler
import noobroutes.features.floor7.autop3.CommandGenerated
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingBase
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.Scheduler
import noobroutes.utils.Utils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.modMessage

class HClipRing(
    ringBase: RingBase,
    var walk: Boolean = false,
    var insta: Boolean = false
) : Ring(ringBase, RingType.H_CLIP) {
    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {
            return HClipRing(generateRingBaseFromArgs(args), getWalkFromArgs(args))
        }
    }

    init {
        addBoolean("walk", {walk}, {walk = it})
    }

    override fun doRing() {
        super.doRing()

        if (insta) {
            if (mc.thePlayer.motionX != 0.0 || mc.thePlayer.motionZ != 0.0) return modMessage("must be not moving in x/z")

            setMaxSpeed()

            if (walk) Scheduler.schedulePostMoveEntityWithHeadingTask { AutoP3MovementHandler.setDirection(yaw) }
            return
        }

        mc.thePlayer.motionX = 0.0
        mc.thePlayer.motionZ = 0.0

        Scheduler.schedulePostMoveEntityWithHeadingTask { setMaxSpeed() }

        if (walk) Scheduler.schedulePostMoveEntityWithHeadingTask(1) { AutoP3MovementHandler.setDirection(yaw) }
    }

    private fun setMaxSpeed() {
        mc.thePlayer.motionX = AutoP3MovementHandler.DEFAULT_SPEED * PlayerUtils.getPlayerWalkSpeed() * Utils.xPart(yaw)
        mc.thePlayer.motionZ = AutoP3MovementHandler.DEFAULT_SPEED * PlayerUtils.getPlayerWalkSpeed() * Utils.zPart(yaw)
    }
}