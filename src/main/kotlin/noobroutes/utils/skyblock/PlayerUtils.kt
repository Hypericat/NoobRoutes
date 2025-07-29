package noobroutes.utils.skyblock

import net.minecraft.block.Block
import net.minecraft.client.settings.KeyBinding
import net.minecraft.inventory.ContainerChest
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.Core.mc
import noobroutes.events.impl.PacketEvent
import noobroutes.mixin.accessors.TimerFieldAccessor
import noobroutes.utils.PacketUtils
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import org.lwjgl.input.Keyboard
import kotlin.math.pow
import kotlin.math.sqrt

object PlayerUtils {
    var shouldBypassVolume = false

    const val STAND_EYE_HEIGHT = 1.6200000047683716
    const val SNEAK_EYE_HEIGHT = 1.5399999618530273
    const val SNEAK_HEIGHT_INVERTED = 0.0800000429153443

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

    fun airClick(){
        devMessage("Clicked: ${System.currentTimeMillis()}")
        PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
    }

    fun stopVelocity(){
        mc.thePlayer.setVelocity(0.0, mc.thePlayer.motionY, 0.0)
    }
    fun setMotion(x: Double, z: Double){
        mc.thePlayer.motionX = x
        mc.thePlayer.motionZ = z
    }

    fun setPosition(x: Double, z: Double){
        mc.thePlayer.setPosition(x, mc.thePlayer.posY, z)
    }

    inline val movementKeysPressed: Boolean get() = playerControlsKeycodes.any { Keyboard.isKeyDown(it) } && mc.currentScreen == null




    /**
     * Displays an alert on screen and plays a sound
     *
     * @param title String to be displayed.
     * @param playSound Toggle for sound.
     *
     * @author Odtheking, Bonsai
     */
    fun alert(title: String, time: Int = 20, color: Color = Color.WHITE, playSound: Boolean = true, displayText: Boolean = true) {
        if (playSound) playLoudSound("note.pling", 100f, 1f)
        if (displayText) Renderer.displayTitle(title , time, color = color)
    }

    inline val posX get() = mc.thePlayer?.posX ?: 0.0
    inline val posY get() = mc.thePlayer?.posY ?: 0.0
    inline val posZ get() = mc.thePlayer?.posZ ?: 0.0

    fun getPositionString() = "x: ${posX.toInt()}, y: ${posY.toInt()}, z: ${posZ.toInt()}"

    private var lastGuiClickSent = 0L

    @SubscribeEvent
    fun onPacketSend(event: PacketEvent.Send) {
        when (event.packet) {
            is C0EPacketClickWindow -> lastGuiClickSent = System.currentTimeMillis()
        }
    }

    fun windowClick(slotId: Int, button: Int, mode: Int) {
        if (lastGuiClickSent + 45 > System.currentTimeMillis())
        mc.thePlayer?.openContainer?.let {
            if (it !is ContainerChest || slotId !in 0 until it.inventorySlots.size) return
            mc.netHandler?.networkManager?.sendPacket(C0EPacketClickWindow(it.windowId, slotId, button, mode, it.inventory[slotId], it.getNextTransactionID(mc.thePlayer?.inventory)))
        }
    }

    fun windowClick(slotId: Int, clickType: ClickType) {
        when (clickType) {
            is ClickType.Left -> windowClick(slotId, 0, 0)
            is ClickType.Right -> windowClick(slotId, 1, 0)
            is ClickType.Middle -> windowClick(slotId, 2, 3)
            is ClickType.Shift -> windowClick(slotId, 0, 1)
        }
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


    fun setSneak(state: Boolean){
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, state)
        lastSetSneakState = state
    }

    fun resyncSneak(){
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.keyCode))
    }

    fun unSneak(){
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, false)
        lastSetSneakState = false
    }

    fun sneak(){
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, true)
        lastSetSneakState = true
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

    fun unPressKeys() {
        Keyboard.enableRepeatEvents(false)
        keyBindings.forEach { KeyBinding.setKeyBindState(it.keyCode, false) }
    }

    fun rePressKeys() {
        keyBindings.forEach { KeyBinding.setKeyBindState(it.keyCode, Keyboard.isKeyDown(it.keyCode)) }
    }

    val gameSpeedAccessor = mc as TimerFieldAccessor

    fun setGameSpeed(speed: Float){
        gameSpeedAccessor.timer.timerSpeed = speed
        gameSpeedAccessor.timer.updateTimer()
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