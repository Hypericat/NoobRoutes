package com.github.wadey3636.noobroutes.features.move

import me.noobmodcore.features.Module
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent

object InstantSlow : Module("Instant Slow", description = "Instantly stops momentum when you are not pressing wasd while in creative fly.") {

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (mc.thePlayer == null || !mc.thePlayer.capabilities.isFlying) return

        val keyBindings = listOf(
            mc.gameSettings.keyBindForward,
            mc.gameSettings.keyBindBack,
            mc.gameSettings.keyBindLeft,
            mc.gameSettings.keyBindRight
        )

        if (keyBindings.none { it.isKeyDown }) {
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
        }


    }



}