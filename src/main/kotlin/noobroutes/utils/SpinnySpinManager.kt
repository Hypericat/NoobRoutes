package noobroutes.utils

import gg.essential.elementa.utils.Vector2f
import net.minecraft.util.MovementInputFromOptions
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core.mc
import noobroutes.utils.Utils.isStart
import kotlin.math.cos
import kotlin.math.sin

object SpinnySpinManager {
    private var renderRotation: LookVec = LookVec(0f, 0f);
    private var silentRotation: LookVec = LookVec(0f, 0f);

    private var forwardRemainder: Float = 0f;
    private var strafeRemainder: Float = 0f;
    private var lastRotationDeltaYaw: Float = 0f;

    private var desyncCounter: Int = 0;
    private var desync: Boolean = false;

    fun rotate(yaw: Float, pitch: Float, silent: Boolean) {
        if (silent || !shouldDesync()) {
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

        val step = mc.gameSettings.mouseSensitivity * 0.6f + 0.2f
        val gcd = step * step * step * 8f

        val betterYaw = yaw?.let { (it / gcd).toInt() * gcd }
        val betterPitch = pitch?.let { (it / gcd).toInt() * gcd }

        silentRotation = LookVec(betterYaw ?: mc.thePlayer.rotationYaw, betterPitch ?: mc.thePlayer.rotationPitch);
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
    fun handleMouseMovements(yaw: Float, pitch: Float) : Boolean {
        if (!shouldDesync()) return false;

        val oldYaw: Float = renderRotation.yaw
        val oldPitch: Float = renderRotation.pitch

        val newYaw: Float  = (renderRotation.yaw.toDouble() + yaw.toDouble() * 0.15).toFloat()
        val newPitch: Float  = (renderRotation.pitch.toDouble() - pitch.toDouble() * 0.15).toFloat().coerceIn(-90f, 90f)

        renderRotation = LookVec(newYaw, newPitch)
        mc.thePlayer.prevRotationPitch = oldPitch; // ???
        mc.thePlayer.prevRotationYaw = oldYaw; // ???
        return true;
    }

    private fun rotateVector(x: Float, y: Float, deltaYaw: Float) : Vector2f {
        val radians = Math.toRadians(deltaYaw.toDouble());
        val sin = sin(radians)
        val cos = cos(radians)
        return Vector2f((x * cos + y * sin).toFloat(), (y * cos - x * sin).toFloat())
    }

    fun adjustMovementInputs(movementInputs: MovementInputFromOptions) {
        if (!shouldDesync()) return;

        if (movementInputs.moveForward == 0f && movementInputs.moveStrafe == 0f) {
            forwardRemainder = 0f;
            strafeRemainder = 0f;
            lastRotationDeltaYaw = renderRotation.yaw - silentRotation.yaw;
            return;
        }

        // Rotate the remainder to reflect the new yaw
        val currentDeltaYaw = renderRotation.yaw - silentRotation.yaw
        val deltaYaw: Float = currentDeltaYaw - lastRotationDeltaYaw;

        if (deltaYaw != 0.0f) {
            val newRemainder = rotateVector(forwardRemainder, strafeRemainder, deltaYaw);
            forwardRemainder = newRemainder.x
            strafeRemainder  = newRemainder.y
        }

        lastRotationDeltaYaw = currentDeltaYaw

        val newDir: Vector2f = rotateVector(movementInputs.moveForward, movementInputs.moveStrafe, currentDeltaYaw)
        val newForward = (newDir.x - forwardRemainder).coerceIn(-1.0F, 1.0F);
        val newStrafe = (newDir.y - strafeRemainder).coerceIn(-1.0F, 1.0F);

        movementInputs.moveForward = newForward.round(0).toFloat();
        movementInputs.moveStrafe = newStrafe.round(0).toFloat();

        forwardRemainder = (movementInputs.moveForward - newForward);
        strafeRemainder = (movementInputs.moveStrafe - newStrafe);
    }


    @SubscribeEvent
    fun onFrame(event: TickEvent.RenderTickEvent) {
        if (mc.thePlayer == null || !shouldDesync()) return
        //serversideRotate((Math.random() * 320 - 180).toFloat(), (Math.random() * 180 - 90).toFloat()) // Spin bot

        if (event.phase == TickEvent.Phase.START) {
            mc.thePlayer.renderArmYaw = renderRotation.yaw
            mc.thePlayer.renderArmPitch = renderRotation.pitch
            mc.thePlayer.prevRenderArmYaw = renderRotation.yaw
            mc.thePlayer.prevRenderArmPitch = renderRotation.pitch

            mc.thePlayer.prevRotationPitch = renderRotation.pitch; // so that it doesn't bug out in GUIs
            mc.thePlayer.prevRotationYaw = renderRotation.yaw; // so that it doesn't bug out in GUIs
            setRotation(renderRotation)
            return
        }

        // frame end now :) - Wadey
        setRotation(silentRotation)
    }

    fun getClientSideRotation() : LookVec {
        return renderRotation;
    }

    fun getServerSideRotation() : LookVec {
        return silentRotation;
    }

    fun setDesync(bl: Boolean) {
        if (bl) {
            enableDesync();
            return;
        }
        disableDesync(false)
    }

    fun shouldDesync() : Boolean {
        return desync || desyncCounter > 0;
    }

    fun getDesyncTicks() : Int {
        return desyncCounter;
    }

    fun getDesyncStatus() : Boolean {
        return desync;
    }

    fun enableDesync() {
        if (desync || mc.thePlayer == null) return;
        if (!shouldDesync()) {
            serversideRotate(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
            clientsideRotate(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
        }

        desync = true;
    }

    fun disableDesync(force: Boolean = false) {
        desync = false;
        if (force)
            desyncCounter = 0;

        if (!shouldDesync()) {
            serversideRotate(renderRotation.yaw, renderRotation.pitch)
        }
    }

    fun requestDesync(ticks: Int) {
        if (ticks < 0 || desyncCounter >= ticks) return
        if (!shouldDesync()) {
            serversideRotate(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
            clientsideRotate(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)
        }
        desyncCounter = ticks;
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.isStart || mc.thePlayer == null || desyncCounter <= 0) return
        desyncCounter--;
        if (!shouldDesync()) {
            serversideRotate(renderRotation.yaw, renderRotation.pitch)
        }
    }
}