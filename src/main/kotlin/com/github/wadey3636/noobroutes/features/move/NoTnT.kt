package com.github.wadey3636.noobroutes.features.move

import com.github.wadey3636.noobroutes.features.floor7.AutoP3
import me.noobmodcore.events.impl.PacketEvent
import me.noobmodcore.features.Category
import me.noobmodcore.features.Module
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

object NoTnT: Module(
    name = "No TnT kb",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "cancels tnt kb in f7/m7"
) {
    @SubscribeEvent
    fun onKb(event: PacketEvent.Receive) {
        if (event.packet is S27PacketExplosion && AutoP3.inBoss) event.isCanceled = true
    }
}