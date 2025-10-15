package noobroutes.utils

import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.EntityRenderer
import net.minecraft.util.MouseHelper
import net.minecraftforge.client.event.EntityViewRenderEvent
import net.minecraftforge.client.event.RenderWorldEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import noobroutes.Core.mc

object SpinnySpinManager {

    private var renderRotation: LookVec = LookVec(0f, 0f);
    private var silentRotation: LookVec = LookVec(0f, 0f);

    fun rotate(yaw: Float, pitch: Float, silent: Boolean) {
        if (silent) {
            serversideRotate(yaw, pitch)
            return
        }

        clientsideRotate(yaw, pitch)
    }

    fun clientsideRotate(yaw: Float?, pitch: Float?) {
        renderRotation = LookVec(yaw ?: renderRotation.yaw, pitch ?: renderRotation.pitch);
    }

    fun serversideRotate(yaw: Float?, pitch: Float?) {
        setRotation(yaw, pitch)
    }

    private fun setRotation(yaw: Float?, pitch: Float?) {
        yaw?.let { mc.thePlayer.rotationYaw = yaw }
        pitch?.let { mc.thePlayer.rotationPitch = pitch.coerceIn(-90f, 90f) }
    }

    private fun setRotation(rotation: LookVec) {
        mc.thePlayer.rotationYaw = rotation.yaw;
        mc.thePlayer.rotationPitch = rotation.pitch;
    }

    @SubscribeEvent
    fun onFrame(event: RenderWorldEvent) {
        setRotation(renderRotation)

    }

    @SubscribeEvent
    fun onFramePost(event: RenderWorldLastEvent) {
        renderRotation = LookVec(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
        setRotation(silentRotation)

    }
}