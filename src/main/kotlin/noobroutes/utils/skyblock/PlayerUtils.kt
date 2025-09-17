package noobroutes.utils.skyblock

import net.minecraft.block.Block
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.settings.KeyBinding
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.Core.mc
import noobroutes.events.impl.PacketEvent
import noobroutes.features.render.FreeCam
import noobroutes.mixin.accessors.EntityLivingBaseAccessor
import noobroutes.mixin.accessors.TimerFieldAccessor
import noobroutes.utils.PacketUtils
import noobroutes.utils.render.RenderUtils.renderVec
import org.lwjgl.input.Keyboard
import kotlin.math.pow
import kotlin.math.sqrt

object PlayerUtils {
    var shouldBypassVolume = false

    const val STAND_EYE_HEIGHT = 1.6200000047683716
    const val SNEAK_EYE_HEIGHT = 1.5399999618530273
    const val SNEAK_HEIGHT_INVERTED = 0.0800000429153443

    fun getEffectiveViewPosition(): Vec3 {
        return if (FreeCam.enabled) {
            FreeCam.getPlayerPositionVec().subtract(0.0, mc.thePlayer.eyeHeight.toDouble(), 0.0)
        } else {
            mc.thePlayer.renderVec
        }
    }

    /**
     * Plays a sound at a specified volume and pitch, bypassing the default volume setting.
     *
     * @param sound The identifier of the sound to be played.
     * @param volume The volume at which the sound should be played.
     * @param pitch The pitch at which the sound should be played.66666
     *
     * @author Aton
     */
    fun playLoudSound(sound: String?, volume: Float, pitch: Float, pos: Vec3? = null) {
        mc.addScheduledTask {
            shouldBypassVolume = true
            mc.theWorld?.playSound(pos?.xCoord ?: mc.thePlayer.posX, pos?.yCoord ?: mc.thePlayer.posY, pos?.zCoord  ?: mc.thePlayer.posZ, sound, volume, pitch, false)
            shouldBypassVolume = false
        }
    }

    inline val EntityPlayerSP.lastPosition: Vec3 get() = Vec3(this.lastTickPosX, this.lastTickPosY, this.lastTickPosZ)

    var slot = -1

    /**
     * Swings the player's arm, does not send a C0APacketAnimation
     */
    fun swing(){
        if (!mc.thePlayer.isSwingInProgress || mc.thePlayer.swingProgressInt >= (mc.thePlayer as EntityLivingBaseAccessor).callGetArmSwingAnimationEnd() / 2 || mc.thePlayer.swingProgressInt < 0) {
            mc.thePlayer.swingProgressInt = -1
            mc.thePlayer.isSwingInProgress = true
        }
    }

    fun getMotionVector(): Vec3 {
        return Vec3(mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ)
    }

    fun airClick(){
        if (isZeroTickSwapping()) return
        PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
    }

    fun stopVelocity(){
        mc.thePlayer.setVelocity(0.0, mc.thePlayer.motionY, 0.0)
    }

    fun setMotionVector(vec: Vec3) {
        mc.thePlayer.motionX = vec.xCoord
        mc.thePlayer.motionY = vec.yCoord
        mc.thePlayer.motionZ = vec.zCoord
    }

    fun setMotion(x: Double, z: Double){
        mc.thePlayer.motionX = x
        mc.thePlayer.motionZ = z
    }

    fun setPosition(vec: Vec3){
        mc.thePlayer.setPosition(vec.xCoord, vec.yCoord, vec.zCoord)
    }

    fun setPosition(x: Double, z: Double){
        mc.thePlayer.setPosition(x, mc.thePlayer.posY, z)
    }

    fun shift(x: Double, z: Double) {
        mc.thePlayer.setPosition(x + posX, posY, z + posZ)
    }

    inline val movementKeysPressed: Boolean get() = (mc.thePlayer.moveForward != 0.0f || mc.thePlayer.moveStrafing != 0.0f || mc.thePlayer.movementInput.jump) && mc.currentScreen == null


    inline val posX get() = mc.thePlayer?.posX ?: 0.0
    inline val posY get() = mc.thePlayer?.posY ?: 0.0
    inline val posZ get() = mc.thePlayer?.posZ ?: 0.0

    fun getPositionString() = "x: ${posX.toInt()}, y: ${posY.toInt()}, z: ${posZ.toInt()}"

    private var lastGuiClickSent = 0L

    @SubscribeEvent
    fun onPacketSend(event: PacketEvent.Send) {
        when (event.packet) {
            is C0EPacketClickWindow -> lastGuiClickSent = System.currentTimeMillis()
            is C09PacketHeldItemChange -> {
                if (!event.isCanceled) slot = event.packet.slotId
            }
        }
    }

    fun isZeroTickSwapping(): Boolean {
        val zeroSwapped = mc.thePlayer.inventory.currentItem != slot
        if (zeroSwapped) modMessage("Tip: zero tick swapping isn't good for the longevity of the account.")
        return zeroSwapped
    }

    fun distanceToPlayer(x: Int, y: Int, z: Int): Double {
        return mc.thePlayer.positionVector.distanceTo(Vec3(x.toDouble(), y.toDouble(), z.toDouble()))
    }


    inline val Vec3.distanceToPlayer2D get() = sqrt((mc.thePlayer.positionVector.xCoord - this.xCoord).pow(2) + (mc.thePlayer.positionVector.zCoord - this.zCoord).pow(2))
    inline val Vec3.distanceToPlayer2DSq get() = (mc.thePlayer.positionVector.xCoord - this.xCoord).pow(2) + (mc.thePlayer.positionVector.zCoord - this.zCoord).pow(2)
    inline val Vec3.distanceToPlayerSq get() = mc.thePlayer.positionVector.squareDistanceTo(this)
    inline val Vec3.distanceToPlayer get() = mc.thePlayer.positionVector.distanceTo(this)
    inline val BlockPos.distanceToPlayer get() = mc.thePlayer.positionVector.distanceTo(Vec3(this))
    inline val BlockPos.distanceToPlayerSq get() = mc.thePlayer.positionVector.squareDistanceTo(Vec3(this))
    var lastSetSneakState = false
    var serverSideSneak = false

    fun setSneak(state: Boolean, serverSide: Boolean = false){
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, state)
        lastSetSneakState = state
        serverSideSneak = serverSide
    }

    fun resyncSneak(){
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.keyCode))
        serverSideSneak = false
    }

    fun unSneak(serverSide: Boolean = false){
        lastSetSneakState = false
        serverSideSneak = serverSide
        if (serverSide) return
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, false)
    }

    fun sneak(serverSide: Boolean = false){
        lastSetSneakState = true
        serverSideSneak = serverSide
        if (serverSide) return
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, true)
    }

    fun getBlockPlayerIsLookingAt(distance: Double = 5.0): Block? {
        val rayTraceResult = mc.thePlayer.rayTrace(distance, 1f)
        return rayTraceResult?.blockPos?.let { mc.theWorld.getBlockState(it).block }
    }

    fun getPlayerWalkSpeed(): Float =
        mc.thePlayer.capabilities.walkSpeed

    // Good boy
    inline val keyBindings get() =  listOf(
        mc.gameSettings.keyBindForward,
        mc.gameSettings.keyBindLeft,
        mc.gameSettings.keyBindRight,
        mc.gameSettings.keyBindBack,
        mc.gameSettings.keyBindJump,
        mc.gameSettings.keyBindSprint
    )

    inline val playerControlsKeycodes get() = keyBindings.map { it.keyCode}

    fun unPressKeys(excludeSpace: Boolean = false) {
        Keyboard.enableRepeatEvents(false)
        keyBindings.forEach {
            if (excludeSpace && it.keyCode == mc.gameSettings.keyBindJump.keyCode) return@forEach
            KeyBinding.setKeyBindState(it.keyCode, false)
        }
    }

    fun rePressKeys(excludeSpace: Boolean = false) {
        keyBindings.forEach {
            if (excludeSpace && it.keyCode == mc.gameSettings.keyBindJump.keyCode) return@forEach
            KeyBinding.setKeyBindState(it.keyCode, Keyboard.isKeyDown(it.keyCode))
        }
    }

    val gameSpeedAccessor = mc as TimerFieldAccessor

    fun setGameSpeed(speed: Float){
        gameSpeedAccessor.timer.timerSpeed = speed
        gameSpeedAccessor.timer.updateTimer()
    }

    fun safeJump() {
        if (mc.thePlayer.onGround) mc.thePlayer.jump()
    }

    fun resetGameSpeed(){
        gameSpeedAccessor.timer.timerSpeed = 1f
        gameSpeedAccessor.timer.updateTimer()
    }

    fun getGameSpeed(): Float {
        return gameSpeedAccessor.timer.timerSpeed
    }

}

sealed class ClickType {
    data object Left   : ClickType()
    data object Right  : ClickType()
    data object Middle : ClickType()
    data object Shift  : ClickType()
}