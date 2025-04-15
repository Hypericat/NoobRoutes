package com.github.wadey3636.noobroutes.features.floor7

import me.defnotstolen.events.impl.MelodyOpenEvent
import me.defnotstolen.events.impl.PacketEvent
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.features.settings.impl.StringSetting
import me.defnotstolen.utils.skyblock.devMessage
import me.defnotstolen.utils.skyblock.modMessage
import me.defnotstolen.utils.skyblock.sendCommand
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

object MelodyMessage: Module(
    name = "Melody Message",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "announces melody open and status in party chat"
) {
    private val message by StringSetting("message", "", description = "message to send")

    private var melodyWindowId = 0
    private var melodyClicks = 0

    @SubscribeEvent
    fun onMelodyOpen(event: MelodyOpenEvent) {
        devMessage("melody start detected")
        melodyWindowId = event.packet.windowId
        melodyClicks = 0
        sendCommand("pc $message started")
    }

    @SubscribeEvent
    fun onClick(event: PacketEvent.Send) {
        if (event.packet !is C0EPacketClickWindow || event.packet.windowId != melodyWindowId) return
        devMessage("melody click detected")
        melodyClicks++
        sendCommand("pc $message $melodyClicks/4")
    }
}