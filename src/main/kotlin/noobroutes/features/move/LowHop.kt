package noobroutes.features.move

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.impl.MoveEntityWithHeadingEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.DevOnly
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.utils.skyblock.LowHopUtils
import org.lwjgl.input.Keyboard
import kotlin.math.cos
import kotlin.math.sin

/**
 * Lowhop (7 Tick) and Disabler taken from raven bs v2
 */

@DevOnly
object LowHop: Module(
    name = "Lowhop",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "lowhop skidded from raven bs v2 because momokohd wouldn't fucking shutup"
) {
    private var startDisablerKey by KeybindSetting("Disable Keybind", Keyboard.KEY_NONE, "starts the disabler").onPress { LowHopUtils.disable() }

    @SubscribeEvent
    fun onMoveEntityPre(event: MoveEntityWithHeadingEvent.Pre) {
        if (mc.thePlayer.motionY.toFloat() == 0.42f && Keyboard.isKeyDown(mc.gameSettings.keyBindJump.keyCode) && LowHopUtils.disabled) {
            LowHopUtils.lowHopThisJump = true
            return
        }

        if (mc.thePlayer.motionY != 0.39) return

        val yawRads = mc.thePlayer.rotationYaw * Math.PI / 180
        val sinYaw = sin(yawRads)
        val cosYaw = cos(yawRads)
        mc.thePlayer.motionX += (mc.thePlayer.moveForward.toInt() * -sinYaw + mc.thePlayer.moveStrafing.toInt() * cosYaw) * mc.thePlayer.capabilities.walkSpeed * 0.4
        mc.thePlayer.motionX += (mc.thePlayer.moveForward.toInt() * cosYaw + mc.thePlayer.moveStrafing.toInt() * sinYaw) * mc.thePlayer.capabilities.walkSpeed * 0.4
    }
}