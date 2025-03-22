package com.github.wadey3636.noobroutes.features

import com.github.wadey3636.noobroutes.utils.PacketUtils
import me.defnotstolen.events.impl.PacketEvent
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.features.settings.impl.BooleanSetting
import me.defnotstolen.ui.clickgui.ClickGUI
import me.defnotstolen.utils.skyblock.modMessage
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import net.minecraft.network.Packet
import me.defnotstolen.utils.render.Color
import me.defnotstolen.utils.render.TextAlign
import me.defnotstolen.utils.render.roundedRectangle
import me.defnotstolen.utils.render.text
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket
import kotlin.math.PI
import kotlin.math.sin

object BlinkKeybind: Module(
    name = "Blink Keybind",
    Keyboard.KEY_NONE,
    category = Category.RENDER,
    description = "A keybinding for Blink (Non-preset actions)"
) {
    private val legit by BooleanSetting("Legit", default = false, description = "reduces ban risk of this feature to 0")
    private var ticks = 0
    private var blinkTime = System.currentTimeMillis()
    private val cancelledPackets = mutableListOf<Packet<*>>()
    override fun onDisable() {
        super.onDisable()
        cancelledPackets.forEach { PacketUtils.sendPacket(it) }
        cancelledPackets.clear()
        ticks = 0
    }

    override fun onEnable() {
        if (mc.currentScreen is ClickGUI) {
            onDisable()
            toggle()
            return modMessage("Enable using the Keybind")
        }
        if (!legit) {
            cancelledPackets.clear()
            ticks = 0
        }
        else blinkTime = System.currentTimeMillis()
        super.onEnable()
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Send) {
        if (legit) return
        if (event.packet.toString().contains("server") || event.packet is S38PacketPlayerListItem || event.packet is FMLProxyPacket) return
        event.isCanceled = true
        if(event.packet is C03PacketPlayer) ticks++
        cancelledPackets.add(event.packet)
    }
    @SubscribeEvent
    fun onRender(event: RenderGameOverlayEvent) {
        val resolution = ScaledResolution(mc)
        if (!legit) text("Blinked for $ticks ticks", resolution.scaledWidth / 2, resolution.scaledHeight / 2.5, Color.WHITE, 10, align = TextAlign.Middle)
        else {
            val passedTime = System.currentTimeMillis() - blinkTime
            if (passedTime > 300) {
                toggle()
                onDisable()
                return
            }
            val height = (sin(passedTime.toDouble() * PI / 300) * (resolution.scaledHeight / 2))
            roundedRectangle(0, 0, resolution.scaledWidth, height.toFloat(), Color.BLACK, edgeSoftness = 0)
            roundedRectangle(0, resolution.scaledHeight - height.toFloat(), resolution.scaledWidth, height.toFloat(), Color.BLACK, edgeSoftness = 0)
        }
    }
}