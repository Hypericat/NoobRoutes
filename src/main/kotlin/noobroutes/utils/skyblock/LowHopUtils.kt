package noobroutes.utils.skyblock

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.MoveEntityWithHeadingEvent
import noobroutes.events.impl.WorldChangeEvent

/**
 * Lowhop (7 Tick) and Disabler taken from raven bs v2
 */

object LowHopUtils {
    var disabled = false

    private var disabling = false

    private var inAirTicks = 0

    private var tickCounter = 0

    var lowHopThisJump = false

    @SubscribeEvent
    fun onMotionPre(event: MotionUpdateEvent.Pre) {
        if (!disabling) return

        if (event.onGround) inAirTicks = 0
        else inAirTicks++

        if (inAirTicks < 10) return

        mc.thePlayer.setVelocity(0.0, 0.0, 0.0)

        if (mc.thePlayer.ticksExisted % 2 == 0) {
            event.x += 0.075
            event.z += 0.075
        }

        tickCounter++

        if (tickCounter < 115) return

        disabling = false
        disabled = true
        modMessage("can lowhop now")
    }

    @SubscribeEvent
    fun onMovePre(event: MoveEntityWithHeadingEvent.Pre) {
        if (mc.thePlayer == null || !lowHopThisJump) return

        if (!disabled) {
            modMessage("cant lowhop not disabled")
            lowHopThisJump = false
            return
        }

        when (mc.thePlayer.motionY) {
            0.33319999363422365 -> {
                mc.thePlayer.motionY = 0.39
            }
            0.2193240100631715 -> mc.thePlayer.motionY -= 0.13
            0.009137530039749452 -> mc.thePlayer.motionY -= 0.2
        }
    }

    @SubscribeEvent
    fun onMovePost(event: MoveEntityWithHeadingEvent.Post) {
        if (lowHopThisJump && mc.thePlayer.onGround) lowHopThisJump = false
    }

    fun disable() {
        if (disabled) {
            disabling = false
            modMessage("already disabled")
            return
        }

        if (mc.isSingleplayer) {
            modMessage("disabled cause ure in singleplayer")
            disabled = true
            return
        }

        if (!mc.thePlayer.onGround) return modMessage("must be on ground")

        disabling = true
        mc.thePlayer.jump()
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldChangeEvent) {
        disabled = false
        disabling = false
        tickCounter = 0
        inAirTicks = 0
    }
}