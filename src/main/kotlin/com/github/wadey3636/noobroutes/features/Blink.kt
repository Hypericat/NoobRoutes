package com.github.wadey3636.noobroutes.features


import com.github.wadey3636.noobroutes.utils.AutoP3Utils
import me.odinmain.events.impl.PacketEvent
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.Renderer
import me.odinmain.utils.render.TextAlign
import me.odinmain.utils.render.text
import me.odinmain.utils.skyblock.modMessage
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import kotlin.math.sin

object Blink: Module (
    name = "Blink",
    Keyboard.KEY_NONE,
    category = Category.RENDER,
    description = "Blink"
    ) {
    data class BlinkWaypoints (val coords: Vec3 = mc.thePlayer.positionVector, val length: Int, var active: Boolean = false)
    private val blinkStarts = mutableListOf<BlinkWaypoints>()

    private var cancelled = 0

    private var recording = false
    private var recordingLength = 0
    private val recordedPackets = mutableListOf<C04PacketPlayerPosition>()

    fun blinkCommand(args: Array<out String>) {
        if (args.size < 2) return modMessage("need args")
        when (args[1]) {
            "add" -> {
                try {
                    val length = args[2].toInt()
                    blinkStarts.add(BlinkWaypoints(length = length))
                    modMessage(blinkStarts.size)
                } catch (e: Exception) {
                    modMessage("need length arg")
                }
            }
            else -> modMessage("not an option")
        }
    }

    @SubscribeEvent
    fun canceller(event: PacketEvent) {
        if (event.packet !is C03PacketPlayer) return
        if (event.packet is C04PacketPlayerPosition || event.packet is C06PacketPlayerPosLook) {
            if(cancelled > 0) cancelled--
            return
        }
        if (!event.isCanceled) event.isCanceled = true
        cancelled++
    }


    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent) {
        val resolution = ScaledResolution(mc)
        text(cancelled.toString(), resolution.scaledWidth / 2, resolution.scaledHeight / 2.3, Color.WHITE, 13, align = TextAlign.Middle)
    }

    @SubscribeEvent
    fun blinkWaypoints(event: RenderWorldLastEvent) {
        blinkStarts.forEach {
            Renderer.drawCylinder(it.coords.add(Vec3(0.0, 0.5 * sin(System.currentTimeMillis().toDouble()/300) + 0.5 , 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.WHITE, depth = true)
            if (AutoP3.editMode) return
            if (AutoP3Utils.distanceToRing(it.coords) < 0.5 && AutoP3Utils.ringCheckY(it.coords) && it.active) {
                startRecording(it)
                it.active = false
            }
            else if(AutoP3Utils.distanceToRing(it.coords) > 0.5 || !AutoP3Utils.ringCheckY(it.coords)) it.active = true
        }
    }

    private fun startRecording(waypoint: BlinkWaypoints) {

    }

    @SubscribeEvent
    fun recorder(event: PacketEvent) {
        if (!recording || (event.packet !is C04PacketPlayerPosition && event.packet !is C06PacketPlayerPosLook)) return
        if (event.packet is C04PacketPlayerPosition) recordedPackets.add(event.packet)
        else if (event.packet is C06PacketPlayerPosLook) recordedPackets.add(C04PacketPlayerPosition(event.packet.positionX, event.packet.positionY, event.packet.positionZ, event.packet.isOnGround)) //i need the else if because otherwise kotlin doesnt know know its a c06
        if (recordedPackets.size >= recordingLength) {
            recording = false
        }
    }

}