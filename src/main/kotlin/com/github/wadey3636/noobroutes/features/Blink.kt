package com.github.wadey3636.noobroutes.features

import me.odinmain.events.impl.PacketEvent
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.utils.render.Color
import me.odinmain.utils.skyblock.modMessage
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraftforge.client.event.GuiScreenEvent.DrawScreenEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import me.odinmain.utils.render.RenderUtils.drawCylinder
import me.odinmain.utils.render.RenderUtils.renderVec
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldEvent
import net.minecraftforge.client.event.RenderWorldLastEvent

object Blink: Module (
    name = "Blink",
    Keyboard.KEY_NONE,
    category = Category.RENDER,
    description = "Blink"
    ) {

    private var cancelled = 0

    @SubscribeEvent
    fun canceller(event: PacketEvent) {
        if (event.packet !is C03PacketPlayer) return
        if (event.packet is C04PacketPlayerPosition || event.packet is C06PacketPlayerPosLook) {
            if(cancelled > 0) cancelled--
            modMessage(cancelled)
            return
        }
        if (!event.isCanceled) event.isCanceled = true
        cancelled++
        modMessage(cancelled)
    }

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        drawCylinder(
            pos = mc.thePlayer.renderVec,
            baseRadius = 0.5,
            topRadius = 0.5,
            height = 0.01,
            slices = 24,
            stacks = 1,
            rot1 = 90,
            rot2 = 0,
            rot3 = 0,
            color = Color.RED,
            depth = true,
        )
    }
}