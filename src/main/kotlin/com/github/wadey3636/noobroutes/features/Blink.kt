package com.github.wadey3636.noobroutes.features

import me.odinmain.events.impl.PacketEvent
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.utils.skyblock.modMessage
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

object Blink: Module (
    name = "Blink",
    Keyboard.KEY_NONE,
    category = Category.RENDER,
    description = "Blink"
    ) {
    private var cancelled = 0
    @SubscribeEvent
    fun onC03(event: PacketEvent) {
        if (event.packet !is C03PacketPlayer) return
        modMessage(cancelled)
        if (event.packet is C04PacketPlayerPosition || event.packet is C06PacketPlayerPosLook) {
            if(cancelled > 0) cancelled--
            return
        }
        if (!event.isCanceled) event.isCanceled = true
        cancelled++

    }
}