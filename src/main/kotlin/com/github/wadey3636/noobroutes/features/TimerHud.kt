package com.github.wadey3636.noobroutes.features

import me.defnotstolen.Core
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.utils.render.Color
import me.defnotstolen.utils.render.TextAlign
import me.defnotstolen.utils.render.text
import me.defnotstolen.utils.skyblock.modMessage
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

object TimerHud: Module(
    name = "Stopwatch",
    Keyboard.KEY_NONE,
    category = Category.RENDER,
    description = "times shit based of a keybind"
) {
    private var startTime = System.currentTimeMillis()

    override fun onKeybind() {
        toggle()
    }

    override fun onEnable() {
        modMessage("Timer started")
        startTime = System.currentTimeMillis()
        super.onEnable()
    }

    override fun onDisable() {
        modMessage("Timer stopped")
        val rounded = "%.2f".format((System.currentTimeMillis() - startTime).toDouble() / 1000)
        val ticks = "%.0f".format(((System.currentTimeMillis() - startTime) * 20.0).toDouble() / 1000)
        modMessage("$rounded seconds passed, $ticks ticks passed")
        super.onDisable()
    }

    @SubscribeEvent
    fun onRender(event: RenderGameOverlayEvent) {
        val rounded = "%.2f".format((System.currentTimeMillis() - startTime).toDouble() / 1000) //why is there no toFixed
        val resolution = ScaledResolution(Core.mc)
        text(rounded, resolution.scaledWidth / 1.7, resolution.scaledHeight / 2, Color.WHITE, 13)
    }

    @SubscribeEvent
    fun onUnload(event: WorldEvent.Unload) {
        onDisable()
    }
}