package com.github.wadey3636.noobroutes.features

import me.defnotstolen.events.impl.PacketEvent
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.features.settings.impl.DualSetting
import me.defnotstolen.features.settings.impl.NumberSetting
import me.defnotstolen.utils.equal
import me.defnotstolen.utils.render.Color
import me.defnotstolen.utils.render.Renderer
import me.defnotstolen.utils.render.Renderer.drawBox
import me.defnotstolen.utils.render.Renderer.drawCylinder
import me.defnotstolen.utils.skyblock.modMessage
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.lwjgl.input.Keyboard

object Trail: Module(
    name = "Trail",
    Keyboard.KEY_NONE,
    category = Category.RENDER,
    description = "shows previous positions"
) {
    private val trailDistance by NumberSetting(name = "Length", description = "length duh", min = 1, max = 1000, default = 400)
    private val mode by DualSetting(name = "trail mpde", description = "wether to show line or individual positions", default = false, left = "Line", right = "Boxes")

    private var positions = mutableListOf<Vec3>()

    @SubscribeEvent
    fun onTick(event: PacketEvent.Send) {
        if (mc.thePlayer == null) return
        if (event.packet !is C03PacketPlayer) return
        if (event.isCanceled) return
        val posVec = getVec3(event.packet)
        if (event.packet !is C04PacketPlayerPosition && event.packet !is C06PacketPlayerPosLook) return
        if (positions.size == 0 || !positions[positions.size-1].equal(posVec)) positions.add(posVec)
        positions = positions.subList(positions.size-theSmallerOne(positions.size, trailDistance), positions.size)
    }
    
    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (!mode) {
            val positionsUp = positions.map { it.add(Vec3(0.0, 0.01, 0.0)) }
            Renderer.draw3DLine(positionsUp, Color.CYAN, depth = true)
        }
        else {
            positions.forEach{
                val aaBB = AxisAlignedBB(it.xCoord-0.03, it.yCoord, it.zCoord-0.03, it.xCoord+0.03, it.yCoord+0.06, it.zCoord+0.03)
                drawBox(aaBB, Color.CYAN, depth = true, fillAlpha = 0)
            }
        }
    }

    private fun theSmallerOne(n1: Int, n2: Int): Int {
        return if (n1 >= n2) n2 else n1
    }

    private fun getVec3(packet: C03PacketPlayer): Vec3 {
        return when (packet) {
            is C04PacketPlayerPosition -> Vec3(packet.positionX, packet.positionY, packet.positionZ)
            is C06PacketPlayerPosLook -> Vec3(packet.positionX, packet.positionY, packet.positionZ)
            else -> Vec3(0.0, 0.0, 0.0)
        }

    }
}