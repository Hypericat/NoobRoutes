package com.github.wadey3636.noobroutes.utils

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import me.odinmain.OdinMain.mc

object AutoP3Utils {
    var walking = false

    @SubscribeEvent
    fun walk(event: TickEvent.ClientTickEvent) {
        if (!walking) return
        if (mc.thePlayer.movementInput.moveForward != 0F || mc.thePlayer.movementInput.moveStrafe != 0F) {
            walking = false
            return
        }
        if (event.phase == TickEvent.Phase.START) return
        val speed = mc.thePlayer.capabilities.walkSpeed
        mc.thePlayer.motionX = speed * 2.806 * Utils.xPart(mc.thePlayer.rotationYaw)
        mc.thePlayer.motionZ = speed * 2.806 * Utils.zPart(mc.thePlayer.rotationYaw)
    }
}