package com.github.wadey3636.noobroutes.features.floor7

import com.github.wadey3636.noobroutes.events.impl.MelodyOpenEvent
import com.github.wadey3636.noobroutes.events.impl.PacketEvent
import com.github.wadey3636.noobroutes.events.impl.S2FPacketSetSlotEvent
import com.github.wadey3636.noobroutes.features.Category
import com.github.wadey3636.noobroutes.features.Module
import com.github.wadey3636.noobroutes.features.settings.impl.StringSetting
import com.github.wadey3636.noobroutes.utils.skyblock.devMessage
import com.github.wadey3636.noobroutes.utils.skyblock.modMessage
import com.github.wadey3636.noobroutes.utils.skyblock.sendCommand
import com.github.wadey3636.noobroutes.utils.skyblock.unformattedName
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.network.play.server.S2FPacketSetSlot
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
        if (event.packet !is C0EPacketClickWindow || event.packet.windowId != melodyWindowId || event.packet.windowId == 0) return
        devMessage("melody click detected")
        melodyClicks++
        sendCommand("pc $message $melodyClicks/4")
    }

    //@SubscribeEvent
    fun onSetSlot(event: S2FPacketSetSlotEvent){

        try {
            val clazz = S2FPacketSetSlot::class.java


            val windowIdField = clazz.getDeclaredField("field_149179_a")
            val slotField = clazz.getDeclaredField("field_149178_b")
            val stackField = clazz.getDeclaredField("field_149180_c")

            windowIdField.isAccessible = true
            slotField.isAccessible = true
            stackField.isAccessible = true

            val windowId = windowIdField.getInt(event.packet)
            val slot = slotField.getInt(event.packet)
            val stack = stackField.get(event.packet) as? ItemStack

            if (windowId != melodyWindowId || windowId == 0) {
                modMessage("Slot $slot was updated with: ${stack.unformattedName}")
            }

        } catch (e: Exception) {
            modMessage("broke")
            e.printStackTrace()
        }

        modMessage("")


    }

}