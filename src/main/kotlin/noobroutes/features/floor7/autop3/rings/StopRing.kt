package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.AutoP3MovementHandler
import noobroutes.features.floor7.autop3.CommandGenerated
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingBase
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.skyblock.PlayerUtils

class StopRing(
    ringBase: RingBase = RingBase(),
) : Ring(ringBase, RingType.STOP) {
    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {
            return StopRing(generateRingBaseFromArgs(args))
        }
    }



    override fun run() {
        AutoP3MovementHandler.resetShit()
        PlayerUtils.stopVelocity()
        if (center && !mc.thePlayer.onGround) return
        triggered = true


        if (rotate) {
            AutoP3.setBlinkRotation(yaw, 0f)
        }

        if (center) {
            center()
            if (isAwait) await()
            return
        }

        if (isAwait) {
            await()
            return
        }
    }
    override fun inRing(pos: Vec3): Boolean {
        return checkInBoundsWithSpecifiedHeight(pos, height)
    }
}