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
import me.odinmain.OdinMain.logger
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.TextAlign
import me.odinmain.utils.render.TextPos
import me.odinmain.utils.render.text
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket

object BlinkKeybind: Module(
    name = "Blink Keybind",
    Keyboard.KEY_NONE,
    category = Category.RENDER,
    description = "A keybinding for Blink (Non-preset actions)"
) {
    private var ticks = 0
    private val cancelledPackets = mutableListOf<Packet<*>>()
    override fun onDisable() {
        super.onDisable()
        cancelledPackets.forEach { PacketUtils.sendPacket(it) }
        cancelledPackets.clear()
        ticks = 0
    }

    override fun onEnable() {
        cancelledPackets.clear()
        ticks = 0
        super.onEnable()
    }
    @SubscribeEvent
    fun onPacket(event: PacketEvent.Send) {
        if (event.packet.toString().contains("server") || event.packet is S38PacketPlayerListItem || event.packet is FMLProxyPacket) return
        if (!event.isCanceled) event.isCanceled = true
        if(event.packet is C03PacketPlayer) ticks++
        cancelledPackets.add(event.packet)
    }
    @SubscribeEvent
    fun onRender(event: RenderGameOverlayEvent) {
        val resolution = ScaledResolution(mc)
        text("Blinked for $ticks ticks", resolution.scaledWidth / 2, resolution.scaledHeight / 2.5, Color.WHITE, 10, align = TextAlign.Middle)
    }
}