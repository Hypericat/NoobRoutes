package com.github.wadey3636.noobroutes.features.move

import me.defnotstolen.events.impl.PacketEvent
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

object VertJerry: Module(
    name = "Vertical Jerrychine",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "makes jerry chine only give vertical velo"
) {
    @SubscribeEvent
    fun onJerry(event: PacketEvent.Receive) {
        if (event.packet !is S12PacketEntityVelocity || event.packet.entityID != mc.thePlayer.entityId || event.packet.motionY != 4800) return
        event.isCanceled = true
        mc.thePlayer.setVelocity(mc.thePlayer.motionX, 0.6, mc.thePlayer.motionZ)
    }
}