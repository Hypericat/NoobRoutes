package com.github.wadey3636.noobroutes.features

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
    fun onTick(event: ClientTickEvent) {
        if (mc.thePlayer == null) return
        if (event.phase != TickEvent.Phase.END) return
        if (positions.size == 0 || !positions[positions.size-1].equal(mc.thePlayer.positionVector)) positions.add(mc.thePlayer.positionVector)
        positions = positions.subList(positions.size-theSmallerOne(positions.size, trailDistance), positions.size)
        modMessage(positions.size)
    }
    
    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (!mode) {
            Renderer.draw3DLine(positions, Color.CYAN, depth = true)
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
}