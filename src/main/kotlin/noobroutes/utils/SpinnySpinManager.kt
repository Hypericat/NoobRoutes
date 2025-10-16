package noobroutes.utils

import net.minecraft.util.MovementInputFromOptions
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core.mc
import kotlin.math.roundToInt

object SpinnySpinManager {

    private var renderRotation: LookVec = LookVec(0f, 0f);
    private var silentRotation: LookVec = LookVec(0f, 0f);

    private var forwardRemainder: Float = 0f;
    private var strafeRemainder: Float = 0f;
    private var lastRotationDeltaYaw: Float = 0f;

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
        if (movementInputs.moveForward == 0f && movementInputs.moveStrafe == 0f) {
            forwardRemainder = 0f;
            strafeRemainder = 0f;
            lastRotationDeltaYaw = renderRotation.yaw - silentRotation.yaw;
            return;
        }

        // Rotate the remainder to reflect the new yaw
        val currentDeltaYaw = renderRotation.yaw - silentRotation.yaw
        val yawChange = Math.toRadians((currentDeltaYaw - lastRotationDeltaYaw).toDouble())

        if (yawChange != 0.0) {
            val sin = kotlin.math.sin(yawChange).toFloat()
            val cos = kotlin.math.cos(yawChange).toFloat()

            val rotatedForwardRemainder = forwardRemainder * cos + strafeRemainder * sin
            val rotatedStrafeRemainder  = strafeRemainder * cos - forwardRemainder * sin

            forwardRemainder = rotatedForwardRemainder
            strafeRemainder  = rotatedStrafeRemainder
        }

        lastRotationDeltaYaw = currentDeltaYaw

        val yaw = Math.toRadians((currentDeltaYaw).toDouble())

        val sin: Float = kotlin.math.sin(yaw).toFloat()
        val cos: Float = kotlin.math.cos(yaw).toFloat()

        val oldForward: Float = movementInputs.moveForward;
        val oldStrafe: Float = movementInputs.moveStrafe;

        val newForward: Float = ((oldForward * cos + oldStrafe * sin) - forwardRemainder).coerceIn(-1.0F, 1.0F);
        val newStrafe: Float = ((oldStrafe * cos - oldForward * sin) - strafeRemainder).coerceIn(-1.0F, 1.0F);

        movementInputs.moveForward = newForward.round(0).toFloat();
        movementInputs.moveStrafe = newStrafe.round(0).toFloat();

        forwardRemainder = (movementInputs.moveForward - newForward);
        strafeRemainder = (movementInputs.moveStrafe - newStrafe);
    }


    @SubscribeEvent
    fun onFrame(event: TickEvent.RenderTickEvent) {
        if (mc.thePlayer == null) return
        //serversideRotate((Math.random() * 320 - 180).toFloat(), (Math.random() * 180 - 90).toFloat()) // Spin bot

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