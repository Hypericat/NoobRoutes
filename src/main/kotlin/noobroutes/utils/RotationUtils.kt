package noobroutes.utils


import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core.logger
import noobroutes.Core.mc
import noobroutes.events.impl.InputEvent
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.PacketReturnEvent
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object RotationUtils {
    private const val SNEAKHEIGHT = 0.0800000429153443
    class Rotation(val yaw: Float, val pitch: Float, val silent: Boolean = false, val action: Action?, var continuous: CompletionRequirement? = null)
    enum class CompletionRequirement{
        NonDirectionalC08,
        PreRotate
    }

    enum class Action{
        LeftClick,
        RightClick
    }

    fun yawAndPitchVector(yaw: Float, pitch: Float): Vec3 {
        val f = cos(-yaw * 0.017453292519943295 - PI)
        val f1 = sin(-yaw * 0.017453292519943295 - PI)
        val f2 = -cos(-pitch * 0.017453292519943295)
        val f3 = sin(-pitch * 0.017453292519943295)
        return Vec3(f1*f2, f3, f*f2).bloomNormalize()
    }

    /**
     * Taken from cga
     * @param x X position to aim at.
     * @param y Y position to aim at.
     * @param z Z position to aim at.
     * @param sneaking determines whether the function accounts for sneak height.
     */
    fun getYawAndPitch(x: Double, y: Double, z: Double, sneaking: Boolean = false): Pair<Float, Float> {
        val dx = x - mc.thePlayer.posX
        val dy = y - (mc.thePlayer.posY + mc.thePlayer.eyeHeight - if (sneaking) SNEAKHEIGHT else 0.0)
        val dz = z - mc.thePlayer.posZ

        val horizontalDistance = sqrt(dx * dx + dz * dz)

        val yaw = Math.toDegrees(atan2(-dx, dz))
        val pitch = -Math.toDegrees(atan2(dy, horizontalDistance))

        val normalizedYaw = if (yaw < -180) yaw + 360 else yaw

        return Pair(normalizedYaw.toFloat(), pitch.toFloat())
    }

    fun getYawAndPitchOrigin(originX: Double, originY: Double, originZ: Double, x: Double, y: Double, z: Double, sneaking: Boolean = false): Pair<Float, Float> {
        val dx = x - originX
        val dy = y - (originY + 1.62f - if (sneaking) SNEAKHEIGHT else 0.0)
        val dz = z - originZ

        val horizontalDistance = sqrt(dx * dx + dz * dz)

        val yaw = Math.toDegrees(atan2(-dx, dz))
        val pitch = -Math.toDegrees(atan2(dy, horizontalDistance))

        val normalizedYaw = if (yaw < -180) yaw + 360 else yaw

        return Pair(normalizedYaw.toFloat(), pitch.toFloat())
    }

    fun getYawAndPitchOrigin(origin: Vec3, target: Vec3, sneaking: Boolean = false): Pair<Float, Float>{
        return getYawAndPitchOrigin(origin.xCoord, origin.yCoord, origin.zCoord, target.xCoord, target.yCoord, target.zCoord, sneaking)
    }

    /**
     * Gets the angle to aim at a Vec3.
     *
     * @param pos Vec3 to aim at.
     * @param sneaking determines whether the function accounts for sneak height.
     */
    fun getYawAndPitch(pos: Vec3, sneaking: Boolean = false): Pair<Float, Float> {
        return getYawAndPitch(pos.xCoord, pos.yCoord, pos.zCoord, sneaking)
    }


    fun setAngles(yaw: Float, pitch: Float) {
        mc.thePlayer.rotationYaw = yaw
        mc.thePlayer.rotationPitch = pitch.coerceIn(-90f, 90f)
    }


    fun setAngleToVec3(vec3: Vec3, sneaking: Boolean = false) {
        val angles = getYawAndPitch(vec3.xCoord, vec3.yCoord, vec3.zCoord, sneaking)
        setAngles(angles.first, angles.second)
    }

    @Deprecated(message = "Don't use this, will be removed later, just use a MovementUpdateEvent.Pre")
    fun rotate(yaw: Float, pitch: Float, silent: Boolean = false , action: Action? = null, continuous: CompletionRequirement? = null) {
        currentRotation = Rotation(yaw, pitch, silent,action, continuous)
    }

    var currentRotation: Rotation? = null
    var lastSentC08 = 0L
    var shouldRightClick = false
    var shouldLeftClick = false
    inline val canSendC08 get() = Scheduler.runTime - lastSentC08 > 2
    inline val offset get() = ((Scheduler.runTime % 2 * 2 - 1) * 1e-6).toFloat()
    var targetYaw: Float? = null
    var targetPitch: Float? = null
    var ticksRotated: Long = 0L

    //@SubscribeEvent(priority = EventPriority.LOWEST)
    fun onTick(event: TickEvent.ClientTickEvent){
        if (event.isEnd || mc.thePlayer == null) return
        val rot = currentRotation ?: return
        //if (rot.silent) SilentRotator.doSilentRotation()
        setAngles(rot.yaw + offset, rot.pitch)
        targetYaw = rot.yaw + offset
        targetPitch = rot.pitch
        if (rot.continuous == null) {
            when (rot.action) {
                Action.RightClick -> {
                    shouldRightClick = true
                }
                Action.LeftClick -> {
                    shouldLeftClick = true
                }
                null -> {}
            }
            currentRotation = null
            ticksRotated = 0L
            return
        }
        ticksRotated++
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onMotion(event: MotionUpdateEvent.Pre){
        if (mc.thePlayer == null) return
        val rot = currentRotation ?: return
        //if (rot.silent) SilentRotator.doSilentRotation()
        //setAngles(rot.yaw + offset, rot.pitch)
        event.yaw = rot.yaw + offset
        event.pitch = rot.pitch
        targetYaw = rot.yaw + offset
        targetPitch = rot.pitch
        if (rot.continuous == null) {
            when (rot.action) {
                Action.RightClick -> {
                    shouldRightClick = true
                }
                Action.LeftClick -> {
                    shouldLeftClick = true
                }
                null -> {}
            }
            currentRotation = null
            ticksRotated = 0L
            return
        }
        ticksRotated++
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Send){
        if (event.packet is C08PacketPlayerBlockPlacement && event.packet.placedBlockDirection == 255) {
            lastSentC08 = 0L
            currentRotation?.let {
                if (it.continuous == CompletionRequirement.NonDirectionalC08) {
                    it.continuous = null
                }
            }
        }
    }

    @SubscribeEvent
    fun onKeyInput(event: InputEvent.Keyboard) {
        if (PlayerUtils.playerControlsKeycodes.contains(event.keycode) && currentRotation != null) {
            devMessage("cancelling prerotate")
             try {
                 if (currentRotation!!.continuous == CompletionRequirement.PreRotate) {
                     currentRotation = null
                     ticksRotated = 0L
                     targetPitch = null
                     targetYaw = null
                 }
             } catch (e: Exception) {
                 logger.error(e)
             }
        }
    }


    @SubscribeEvent
    fun onPacketReturn(event: PacketReturnEvent.Send){
        if (event.packet !is C03PacketPlayer || SwapManager.recentlySwapped || !canSendC08) return
        if (shouldRightClick) {
            shouldRightClick = false
            PlayerUtils.airClick()
        }
    }

    fun completePrerotateTask(){
        devMessage("complete prerotate")
        try {
            if (currentRotation != null && currentRotation!!.continuous == CompletionRequirement.PreRotate) {
                currentRotation!!.continuous = null
            }
        } catch (e: Exception) {
            devMessage("Error Occurred Completing Task")
            logger.error(e)
        }

    }

    fun forceCompleteTask(){
        try {
            if (currentRotation != null) {
                currentRotation!!.continuous = null
            }
        } catch (e: Exception) {
            devMessage("Error Occurred Completing Task")
            logger.error(e)
        }
    }






}