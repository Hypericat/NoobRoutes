package noobroutes.features.render

import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraftforge.client.event.RenderLivingEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.utils.Scheduler
import org.lwjgl.input.Keyboard

object RotationVisualizer: Module(
    name = "Rotation Visualizer",
    Keyboard.KEY_NONE,
    category = Category.RENDER,
    description = "shows serverside rotations"
) {
    private var lastRot = Pair(0f, 0f)
    private var almostLastRot = Pair(0f, 0f)
    private var resetPitch: Pair<Float, Float>? = null


    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onC03(event: PacketEvent.Send) {
        if (event.packet is C05PacketPlayerLook || event.packet is C06PacketPlayerPosLook) {
            lastRot = Pair(event.packet.yaw, event.packet.pitch)
            Scheduler.scheduleC03Task { almostLastRot = lastRot }
        }
    }

    @SubscribeEvent
    fun onRenderLiving(event: RenderLivingEvent.Pre<*>) {
        if (event.entity != mc.thePlayer || mc.thePlayer.ridingEntity != null || mc.currentScreen != null) return
        resetPitch = Pair(event.entity.rotationPitch, event.entity.prevRotationPitch)
        event.entity.rotationPitch = lastRot.second
        event.entity.prevRotationPitch = almostLastRot.second
        event.entity.rotationYawHead = lastRot.first
        event.entity.renderYawOffset = lastRot.first
    }

    @SubscribeEvent
    fun afterRender(event: RenderLivingEvent.Post<*>) {
        if (event.entity != mc.thePlayer || resetPitch == null) return
        mc.thePlayer.rotationPitch = resetPitch!!.first
        mc.thePlayer.prevRotationPitch = resetPitch!!.second
        resetPitch = null
    }
}