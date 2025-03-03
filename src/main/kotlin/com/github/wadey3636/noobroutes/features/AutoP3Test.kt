package com.github.wadey3636.noobroutes.features

import com.github.wadey3636.noobroutes.utils.PacketUtils
import me.odinmain.events.impl.PacketEvent
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.utils.skyblock.modMessage
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

object AutoP3Test: Module(
    name = "Gay",
    Keyboard.KEY_NONE,
    category = Category.RENDER,
    description = "Gay0"
) {
    @SubscribeEvent
    fun onC03(event: PacketEvent.Send) {
        if (event.packet is C06PacketPlayerPosLook) {
            modMessage("${event.packet.positionX}, ${event.packet.positionY}, ${event.packet.positionZ}")
            if (!event.isCanceled) event.isCanceled = true
            mc.addScheduledTask {
                PacketUtils.sendPacket(
                    C06PacketPlayerPosLook(
                        69420.0,
                        69420.0,
                        69420.0,
                        0F,
                        0F,
                        false
                    )
                )
            }
        }
        
    }
}