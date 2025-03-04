package com.github.wadey3636.noobroutes.features

import com.github.wadey3636.noobroutes.utils.PacketUtils
import me.odinmain.events.impl.PacketEvent
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.ui.clickgui.ClickGUI
import me.odinmain.utils.skyblock.modMessage
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import net.minecraft.network.Packet

object BlinkKeybind: Module(
    name = "Blink Keybind",
    Keyboard.KEY_NONE,
    category = Category.RENDER,
    description = "A keybinding for Blink (Non-preset actions)"
) {
    private val cancelledPackets = mutableListOf<Packet<*>>()
    override fun onDisable() {
        super.onDisable()
        cancelledPackets.forEach { PacketUtils.sendPacket(it) }
        cancelledPackets.clear()
    }

    override fun onEnable() {
        cancelledPackets.clear()
        super.onEnable()
    }
    @SubscribeEvent
    fun onPacket(event: PacketEvent.Send) {
        if (event.packet.toString().contains("server")) return
        if (!event.isCanceled) event.isCanceled = true
        cancelledPackets.add(event.packet)
        modMessage("cancelled that fucker")
    }
}