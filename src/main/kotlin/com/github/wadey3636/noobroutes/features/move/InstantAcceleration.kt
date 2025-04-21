package com.github.wadey3636.noobroutes.features.move

import com.github.wadey3636.noobroutes.utils.AutoP3Utils
import com.github.wadey3636.noobroutes.utils.ClientUtils
import com.github.wadey3636.noobroutes.utils.Utils
import me.noobmodcore.features.Category
import me.noobmodcore.features.Module
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import org.lwjgl.input.Keyboard

object InstantAcceleration: Module(
    name = "Instant Speed",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "instantly max running speed"
) {

    @SubscribeEvent
    fun onKey(event: InputEvent.KeyInputEvent) {
        if (Keyboard.getEventKey() != Keyboard.KEY_W || !Keyboard.getEventKeyState() || !mc.thePlayer.onGround || mc.thePlayer.isSneaking) return
        val speed = mc.thePlayer.capabilities.walkSpeed * 2.806
        val differenceL = if (Keyboard.isKeyDown(Keyboard.KEY_A)) -45 else 0
        val differenceR = if (Keyboard.isKeyDown(Keyboard.KEY_D)) 45 else 0
        AutoP3Utils.unPressKeys()
        mc.thePlayer.motionX = speed* Utils.xPart(mc.thePlayer.rotationYaw + differenceL + differenceR)
        mc.thePlayer.motionZ = speed* Utils.zPart(mc.thePlayer.rotationYaw + differenceL + differenceR)
        ClientUtils.clientScheduleTask { AutoP3Utils.rePressKeys() }
    }
}