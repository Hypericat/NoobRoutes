package noobroutes.features.floor7.autop3.rings

import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.CommandGenerated
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingBase
import noobroutes.features.floor7.autop3.RingType
import noobroutes.features.move.LavaClip
import noobroutes.ui.editgui.EditGuiBase
import noobroutes.utils.Scheduler
import noobroutes.utils.skyblock.modMessage
import kotlin.math.absoluteValue


class LavaClipRing(
    ringBase: RingBase = RingBase(),
    var length: Double = 0.0
) : Ring(ringBase, RingType.LAVA_CLIP) {
    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {
            if (args.size < 3) {
                modMessage("need a length arg 1-50")
                return null
            }
            val endY = args[2].toDoubleOrNull()?.absoluteValue ?: run {
                modMessage("need a length arg 1-50")
                return null
            }

            return LavaClipRing(generateRingBaseFromArgs(args), endY.coerceAtMost(50.0))
        }
    }

    init {
        addDouble("length", {length}, {length = it})
    }


    override fun doRing() {
        super.doRing()

        mc.thePlayer.motionX = 0.0
        mc.thePlayer.motionZ = 0.0
        LavaClip.ringClip = length
        resetTriggered()
        LavaClip.onEnable()
    }

    private fun resetTriggered() {//TODO: make actual good logic
        triggered = true
        Scheduler.schedulePreTickTask(60) { triggered = false }
    }

    override fun extraArgs(builder: EditGuiBase.EditGuiBaseBuilder) {
        builder.addSlider("Length", 3.0, 40.0, 1.0, 0, {length}, {length = it})
    }
}