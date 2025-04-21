package com.github.wadey3636.noobroutes.features.render

import com.github.wadey3636.noobroutes.features.Blink.movementPackets
import com.github.wadey3636.noobroutes.features.floor7.AutoP3
import com.github.wadey3636.noobroutes.utils.ClientUtils
import me.modcore.events.impl.PacketEvent
import me.modcore.features.Category
import me.modcore.features.Module
import me.modcore.features.settings.impl.BooleanSetting
import me.modcore.features.settings.impl.DualSetting
import me.modcore.features.settings.impl.NumberSetting
import me.modcore.utils.add
import me.modcore.utils.coerceMax
import me.modcore.utils.equal
import me.modcore.utils.render.Color
import me.modcore.utils.render.Renderer
import me.modcore.utils.render.Renderer.drawBox
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

object Trail: Module(
    name = "Trail",
    Keyboard.KEY_NONE,
    category = Category.RENDER,
    description = "shows previous positions"
) {
    private val trailDistance by NumberSetting(name = "Length", description = "length duh", min = 1, max = 1000, default = 100)
    private val mode by DualSetting(name = "Trail mode", description = "Whether to show line or individual positions", default = false, left = "Line", right = "Boxes")
    private val tickDelay by BooleanSetting("Tick Delay", description = "Delays the trail by a tick, makes it look nicer", default = true)

    private var positions = mutableListOf<Vec3>()


    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        positions.clear()
    }

    override fun onDisable() {
        super.onDisable()
        positions.clear()
    }

    @SubscribeEvent
    fun onTick(event: PacketEvent.Send) {
        if (mc.thePlayer == null) return
        if (event.packet !is C03PacketPlayer) return
        if (event.isCanceled) return
        val posVec = getVec3(event.packet)
        if (event.packet !is C04PacketPlayerPosition && event.packet !is C06PacketPlayerPosLook) return
        if (positions.isEmpty() || !positions[positions.size-1].equal(posVec)) {
            if (tickDelay) ClientUtils.clientScheduleTask { positions.add(0, posVec) } else positions.add(0, posVec)
        }
        positions.coerceMax(trailDistance)
    }
    
    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (!mode) {
            val positionsUp = positions.map { it.add(0.0, 0.01, 0.0) }.toMutableList()
            if (movementPackets.isEmpty() || !AutoP3.mode)  {
                val viewEntity = mc.renderViewEntity
                val camX = viewEntity.lastTickPosX + (viewEntity.posX - viewEntity.lastTickPosX) * event.partialTicks
                val camY = viewEntity.lastTickPosY + (viewEntity.posY - viewEntity.lastTickPosY) * event.partialTicks
                val camZ = viewEntity.lastTickPosZ + (viewEntity.posZ - viewEntity.lastTickPosZ) * event.partialTicks
                positionsUp.add(0, Vec3(camX, camY + 0.01, camZ))
            }
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