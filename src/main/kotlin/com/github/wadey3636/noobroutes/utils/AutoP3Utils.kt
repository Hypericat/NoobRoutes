package com.github.wadey3636.noobroutes.utils

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import me.odinmain.OdinMain.mc

object AutoP3Utils {

    @SubscribeEvent
    fun walk(event: TickEvent.ClientTickEvent) {
        if (true) return
        if (event.phase == TickEvent.Phase.START) return
        mc.thePlayer.movementInput.moveForward = 0F
        mc.thePlayer.movementInput.moveStrafe = 0F
    }
}