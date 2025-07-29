package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.CommandGenerated
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingBase
import noobroutes.features.floor7.autop3.RingType
import noobroutes.features.move.LavaClip
import noobroutes.utils.Scheduler
import noobroutes.utils.skyblock.modMessage
import kotlin.math.absoluteValue


class LavaClipRing(
    ringBase: RingBase = RingBase(Vec3(0.0, 0.0, 0.0), 0f, false, false, false, false, false, 1f, 1f),
    var length: Double = 0.0
) : Ring(ringBase, RingType.LAVA_CLIP) {
    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {
            if (args.size < 3) {
                modMessage("need a length arg")
                return null
            }
            val endY = args[2].toDoubleOrNull()?.absoluteValue
            if (endY == null) {
                modMessage("need a length arg")
                return null
            }
            return LavaClipRing(generateRingBaseFromArgs(args), endY)
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
}