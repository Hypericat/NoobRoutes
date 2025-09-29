package noobroutes.features.render

import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.PacketReturnEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.DualSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.Scheduler
import noobroutes.utils.coerceMax
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.render.Renderer.drawBox
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

    private var positions = mutableListOf<C04PacketPlayerPosition>()


    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        positions.clear()
    }

    override fun onDisable() {
        super.onDisable()
        positions.clear()
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onTick(event: PacketReturnEvent.Send) {
        if (mc.thePlayer == null) return
        if (event.packet !is C03PacketPlayer) return
        if (event.packet !is C04PacketPlayerPosition && event.packet !is C06PacketPlayerPosLook) return
        val c04 = getC04(event.packet)
        if (positions.isEmpty() || positions[positions.size-1] != c04) {
            //devMessage("${c04.positionX} ${c04.positionY} ${c04.positionZ}")
            if (tickDelay) Scheduler.schedulePreTickTask { positions.add(0, c04) } else positions.add(0, c04)
        }
        positions.coerceMax(trailDistance)
    }

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        val positionsCopy = positions.toList() //concurrent smth smth error moment
        if (!mode) {
            val positionsUp = positionsCopy.map { Vec3(it.positionX, it.positionY + 0.01, it.positionZ) }.toMutableList()
            val viewEntity = mc.renderViewEntity
            val camX = viewEntity.lastTickPosX + (viewEntity.posX - viewEntity.lastTickPosX) * event.partialTicks
            val camY = viewEntity.lastTickPosY + (viewEntity.posY - viewEntity.lastTickPosY) * event.partialTicks
            val camZ = viewEntity.lastTickPosZ + (viewEntity.posZ - viewEntity.lastTickPosZ) * event.partialTicks
            positionsUp.add(0, Vec3(camX, camY + 0.01, camZ))
            Renderer.draw3DLine(positionsUp, Color.CYAN, depth = true)
        } else {
            positionsCopy.forEach {
                val aaBB = AxisAlignedBB(it.positionX - 0.03, it.positionY, it.positionZ - 0.03, it.positionX + 0.03, it.positionY + 0.06, it.positionZ + 0.03)
                drawBox(aaBB, if (it.isOnGround) Color.RED else Color.CYAN, depth = true, fillAlpha = 0)
            }
        }
    }

    private fun getC04(packet: C03PacketPlayer): C04PacketPlayerPosition {
        return when (packet) {
            is C04PacketPlayerPosition -> packet
            is C06PacketPlayerPosLook -> C04PacketPlayerPosition(packet.positionX, packet.positionY, packet.positionZ, packet.isOnGround)
            else -> C04PacketPlayerPosition(0.0, 0.0, 0.0, false)
        }

    }
}