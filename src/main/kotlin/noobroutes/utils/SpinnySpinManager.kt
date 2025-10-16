package noobroutes.utils

import net.minecraft.util.MovementInputFromOptions
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core.mc
import kotlin.math.roundToInt

object SpinnySpinManager {

    private var renderRotation: LookVec = LookVec(0f, 0f);
    private var prevRenderRot: LookVec = LookVec(0f, 0f);
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
        if (mc.thePlayer == null) return;

        silentRotation = LookVec(yaw ?: mc.thePlayer.rotationYaw, pitch ?: mc.thePlayer.rotationPitch);
        setRotation(silentRotation.yaw, silentRotation.pitch)
    }

    private fun setRotation(yaw: Float?, pitch: Float?) {
        yaw?.let { mc.thePlayer.rotationYaw = yaw }
        pitch?.let { mc.thePlayer.rotationPitch = pitch.coerceIn(-90f, 90f) }
    }

    private fun setRotation(rotation: LookVec) {
        mc.thePlayer.rotationYaw = rotation.yaw;
        mc.thePlayer.rotationPitch = rotation.pitch;
    }

    // Entity : SetAngles
    fun handleMouseMovements(yaw: Float, pitch: Float) {
        val oldYaw: Float = renderRotation.yaw
        val oldPitch: Float = renderRotation.pitch

        val newYaw: Float  = (renderRotation.yaw.toDouble() + yaw.toDouble() * 0.15).toFloat()
        val newPitch: Float  = (renderRotation.pitch.toDouble() - pitch.toDouble() * 0.15).toFloat().coerceIn(-90f, 90f)

        renderRotation = LookVec(newYaw, newPitch)
        mc.thePlayer.prevRotationPitch = oldPitch; // ???
        mc.thePlayer.prevRotationYaw = oldYaw; // ???
    }

    fun adjustMovementInputs(movementInputs: MovementInputFromOptions) {
        val yaw = Math.toRadians((renderRotation.yaw - silentRotation.yaw).toDouble())

        val sin = kotlin.math.sin(yaw)
        val cos = kotlin.math.cos(yaw)

        val oldForward = movementInputs.moveForward;
        val oldStrafe = movementInputs.moveStrafe;
        movementInputs.moveForward = (oldForward * cos + oldStrafe * sin).round(0).toFloat();
        movementInputs.moveStrafe = (oldStrafe * cos - oldForward * sin).round(0).toFloat();
    }


    @SubscribeEvent
    fun onFrame(event: TickEvent.RenderTickEvent) {
        if (mc.thePlayer == null) return
        serversideRotate((Math.random() * 320 - 180).toFloat(), (Math.random() * 180 - 90).toFloat()) // Spin bot

        if (event.phase == TickEvent.Phase.START) {
            //silentRotation = LookVec( mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch);
            mc.thePlayer.prevRotationPitch = renderRotation.pitch; // so that it doesn't bug out in GUIs
            mc.thePlayer.prevRotationYaw = renderRotation.yaw; // so that it doesn't bug out in GUIs
            setRotation(renderRotation)
            return
        }
        //frame end now :)


        //renderRotation = LookVec(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)

        setRotation(silentRotation)
    }

    fun getClientSideRotation() : LookVec {
        return renderRotation;
    }

    fun getServerSideRotation() : LookVec {
        return silentRotation;
    }
}