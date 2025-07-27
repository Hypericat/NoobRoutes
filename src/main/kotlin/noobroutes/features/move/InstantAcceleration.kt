package noobroutes.features.move

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.render.FreeCam
import noobroutes.utils.Scheduler
import noobroutes.utils.Utils
import noobroutes.utils.skyblock.PlayerUtils
import org.lwjgl.input.Keyboard

object InstantAcceleration: Module(
    name = "Instant Speed",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "instantly max running speed"
) {

    @SubscribeEvent
    fun onKey(event: InputEvent.KeyInputEvent) {
        if (Keyboard.getEventKey() != Keyboard.KEY_W || !Keyboard.getEventKeyState() || !mc.thePlayer.onGround || mc.thePlayer.isSneaking || FreeCam.enabled) return
        val speed = mc.thePlayer.capabilities.walkSpeed * 2.806
        val differenceL = if (Keyboard.isKeyDown(Keyboard.KEY_A)) -45 else 0
        val differenceR = if (Keyboard.isKeyDown(Keyboard.KEY_D)) 45 else 0
        PlayerUtils.unPressKeys()
        mc.thePlayer.motionX = speed* Utils.xPart(mc.thePlayer.rotationYaw + differenceL + differenceR)
        mc.thePlayer.motionZ = speed* Utils.zPart(mc.thePlayer.rotationYaw + differenceL + differenceR)
        Scheduler.schedulePreTickTask { PlayerUtils.rePressKeys() }
    }
}