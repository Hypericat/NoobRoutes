package com.github.wadey3636.noobroutes.features.floor7

import com.github.wadey3636.noobroutes.features.floor7.AutoP3.inBoss
import com.github.wadey3636.noobroutes.utils.PacketUtils
import com.github.wadey3636.noobroutes.events.impl.PacketEvent
import com.github.wadey3636.noobroutes.features.Category
import com.github.wadey3636.noobroutes.features.Module
import com.github.wadey3636.noobroutes.utils.noControlCodes
import com.github.wadey3636.noobroutes.utils.skyblock.modMessage
import net.minecraft.client.settings.KeyBinding
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.client.C0CPacketInput
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S1BPacketEntityAttach
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

object InstaMid: Module (
    name = "Insta Mid",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "teleports u back to where u were before necron picked u up (ideally mid)"
) {
    private var cancelling = false
    private var sent = false

    @SubscribeEvent
    fun onSend(event: PacketEvent.Send)  {
        if (!cancelling || (event.packet !is C03PacketPlayer && event.packet !is C0CPacketInput)) return
        event.isCanceled = true
        if (mc.thePlayer.isRiding) return
        if (!sent) {
            cancelling = false
            sent = true
            PacketUtils.sendPacket(C06PacketPlayerPosLook(54.0, 65.0, 76.0, 0F, 0F, false))
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.keyCode))
        }

    }

    @SubscribeEvent
    fun onS1B(event: PacketEvent.Receive) {
        if (event.packet !is S1BPacketEntityAttach || event.packet.entityId != mc.thePlayer.entityId || event.packet.vehicleEntityId < 0 || !inBoss) return
        cancelling = true
        sent = false
        modMessage("instamid")
    }

    @SubscribeEvent
    fun onChat(event: PacketEvent.Receive) {
        if (event.packet !is S02PacketChat || event.packet.type.toInt() != 0) return
        val message = event.packet.chatComponent.unformattedText.noControlCodes
        if (message == "[BOSS] Necron: You went further than any human before, congratulations.") KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, true)
    }

    @SubscribeEvent
    fun onUnload(event: WorldEvent.Unload) { //incase leave during pickup stage
        sent = false
        cancelling = false
    }
}