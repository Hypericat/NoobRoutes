package com.github.wadey3636.noobroutes.features

import com.github.wadey3636.noobroutes.utils.ClientUtils
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
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.Entity
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket
import kotlin.math.PI
import kotlin.math.sin

data class PlayerHit(val entity: Entity, var lastHit: Long)

object BlinkKeybind: Module(
    name = "Blink Keybind",
    Keyboard.KEY_NONE,
    category = Category.RENDER,
    description = "A keybinding for Blink (Non-preset actions)"
) {
    private val legit by BooleanSetting("Legit", default = false, description = "reduces ban risk of this feature to 0")
    private val pvp by BooleanSetting("PvP mode", default = false, description = "ure lagging now")

    private var ticks = 0
    private var blinkTime = System.currentTimeMillis()
    private val cancelledPackets = mutableListOf<Packet<*>>()
    private var skip = 0
    private val hit = mutableListOf<PlayerHit>()

    override fun onDisable() {
        super.onDisable()
        cancelledPackets.forEach { PacketUtils.sendPacket(it) }
        cancelledPackets.clear()
        ticks = 0
    }

    override fun onEnable() {
        if (mc.currentScreen is ClickGUI) {
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
        if (skip > 0) {
            skip--
            return
        }
        if (legit) return
        if (event.packet.toString().contains("server") || event.packet is S38PacketPlayerListItem || event.packet is FMLProxyPacket || event.isCanceled) return
        event.isCanceled = true
        if (event.packet is C03PacketPlayer) ticks++
        cancelledPackets.add(event.packet)
        if (event.packet is C02PacketUseEntity && pvp) {
            if (event.packet.action != C02PacketUseEntity.Action.ATTACK) return
            val entity = event.packet.getEntityFromWorld(mc.theWorld)
            if (hit.any { it.entity == entity && System.currentTimeMillis() - it.lastHit < 500 }) return
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.keyCode, false)
            ClientUtils.clientScheduleTask { KeyBinding.setKeyBindState(mc.gameSettings.keyBindForward.keyCode, Keyboard.isKeyDown(mc.gameSettings.keyBindForward.keyCode)) }
            skip = cancelledPackets.size
            cancelledPackets.forEach { PacketUtils.sendPacket(it) }
            if (hit.any { it.entity == entity }) hit.find { it.entity == entity }?.lastHit = System.currentTimeMillis()
            else hit.add(PlayerHit(entity, System.currentTimeMillis()))
            cancelledPackets.clear()
            ticks = 0
        }
    }
    @SubscribeEvent
    fun onRender(event: RenderGameOverlayEvent) {
        val resolution = ScaledResolution(mc)
        if (!legit) text("Blinked for $ticks ticks", resolution.scaledWidth / 2, resolution.scaledHeight / 2.5, Color.WHITE, 10, align = TextAlign.Middle)
        else {
            val passedTime = System.currentTimeMillis() - blinkTime
            if (passedTime > 300) {
                toggle()
                return
            }
            val height = (sin(passedTime.toDouble() * PI / 300) * (resolution.scaledHeight / 2))
            roundedRectangle(0, 0, resolution.scaledWidth, height.toFloat(), Color.BLACK, edgeSoftness = 0)
            roundedRectangle(0, resolution.scaledHeight - height.toFloat(), resolution.scaledWidth, height.toFloat(), Color.BLACK, edgeSoftness = 0)
        }
    }

    @SubscribeEvent
    fun onUnload(event: WorldEvent.Unload) {
        cancelledPackets.clear()
        ticks = 0
        hit.clear()
        toggle()
    }
}