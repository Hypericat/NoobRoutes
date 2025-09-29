package noobroutes.features.floor7.autop3.rings

import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.*
import noobroutes.ui.editgui.EditGuiBase
import noobroutes.utils.Scheduler
import noobroutes.utils.skyblock.LowHopUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage


class MotionRing(
    ringBase: RingBase = RingBase(),
    var scale: Float = 1f,
    var lowHop: Boolean = false
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
            return MotionRing(generateRingBaseFromArgs(args), scale, getLowFromArgs(args))
        }
    }

    init {
        addFloat("scale", {scale}, {scale = it})
        addBoolean("low", {lowHop}, {lowHop = it})
    }

    override fun doRing() {
        super.doRing()

        if (!mc.thePlayer.onGround) {
            triggered = false
            return
        }

        if (lowHop) {
            if (!LowHopUtils.disabled) return modMessage("Cant cause not disabled")
            Scheduler.scheduleHighestPostMoveEntityWithHeadingTask { LowHopUtils.lowHopThisJump = true }
        }
        mc.thePlayer.jump()

        AutoP3MovementHandler.setDirection(yaw)
        AutoP3MovementHandler.setMotionTicks(0)
        AutoP3MovementHandler.setScale(scale)
    }
    override fun extraArgs(builder: EditGuiBase.EditGuiBaseBuilder) {
        builder.addSlider("Scale", 0.7, 1.0, 0.05, 2, {scale.toDouble()}, {scale = it.toFloat()})
        builder.addSwitch("LowHop", {lowHop}, {lowHop = it})
    }
}