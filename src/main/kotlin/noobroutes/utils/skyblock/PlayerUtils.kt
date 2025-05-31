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
import noobroutes.utils.AutoP3Utils
import noobroutes.utils.PacketUtils
import noobroutes.utils.bloomNormalize
import org.lwjgl.input.Keyboard
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object PlayerUtils {
    var shouldBypassVolume = false

    fun yawPitchVector(yaw: Float, pitch: Float): Vec3 {
        val f = cos(-yaw * 0.017453292 - PI)
        val f1 = sin(-yaw * 0.017453292 - PI)
        val f2 = -cos(-pitch * 0.017453292)
        val f3 = sin(-pitch * 0.017453292)
        return Vec3(f1*f2, f3, f*f2).bloomNormalize()

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

    fun airClick(){
        if (!canSendC08) return
        devMessage("Clicked: ${System.currentTimeMillis()}")
        PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
    }

    fun stopVelocity(){
        mc.thePlayer.setVelocity(0.0, mc.thePlayer.motionY, 0.0)
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
    fun alert(title: String, time: Int = 20, color: noobroutes.utils.render.Color = _root_ide_package_.noobroutes.utils.render.Color.WHITE, playSound: Boolean = true, displayText: Boolean = true) {
        if (playSound) playLoudSound("note.pling", 100f, 1f)
        if (displayText) _root_ide_package_.noobroutes.utils.render.Renderer.displayTitle(title , time, color = color)
    }

    inline val posX get() = mc.thePlayer?.posX ?: 0.0
    inline val posY get() = mc.thePlayer?.posY ?: 0.0
    inline val posZ get() = mc.thePlayer?.posZ ?: 0.0

    fun getPositionString() = "x: ${posX.toInt()}, y: ${posY.toInt()}, z: ${posZ.toInt()}"

    private var lastGuiClickSent = 0L
    var lastC08Sent = 0L
    inline val canSendC08 get() = System.currentTimeMillis() - lastC08Sent > 50


    @SubscribeEvent
    fun onPacketSend(event: PacketEvent.Send) {
        when (event.packet) {
            is C0EPacketClickWindow -> lastGuiClickSent = System.currentTimeMillis()
            is C08PacketPlayerBlockPlacement -> lastC08Sent = System.currentTimeMillis()
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

    fun setSneak(state: Boolean){
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, state)
    }

    fun resyncSneak(){
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.keyCode))
    }

    fun forceUnSneak(){
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, false)
    }

    fun sneak(){
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, true)
    }

    fun getBlockPlayerIsLookingAt(distance: Double = 5.0): Block? {
        val rayTraceResult = mc.thePlayer.rayTrace(distance, 1f)
        return rayTraceResult?.blockPos?.let { mc.theWorld.getBlockState(it).block }
    }


    inline val keyBindings get() =  listOf(
        mc.gameSettings.keyBindForward,
        mc.gameSettings.keyBindLeft,
        mc.gameSettings.keyBindRight,
        mc.gameSettings.keyBindBack,
        mc.gameSettings.keyBindJump
    )

    inline val playerControlsKeycodes get() = keyBindings.map { it.keyCode}

    fun unPressKeys() {
        Keyboard.enableRepeatEvents(false)
        AutoP3Utils.keyBindings.forEach { KeyBinding.setKeyBindState(it.keyCode, false) }
    }

    fun rePressKeys() {
        AutoP3Utils.keyBindings.forEach { KeyBinding.setKeyBindState(it.keyCode, Keyboard.isKeyDown(it.keyCode)) }
    }

}

sealed class ClickType {
    data object Left   : ClickType()
    data object Right  : ClickType()
    data object Middle : ClickType()
    data object Shift  : ClickType()
}