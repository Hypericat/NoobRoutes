package noobroutes.utils.skyblock

import noobroutes.Core.mc
import noobroutes.utils.AutoP3Utils
import noobroutes.utils.PacketUtils
import net.minecraft.client.settings.KeyBinding
import net.minecraft.inventory.ContainerChest
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0EPacketClickWindow
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

object PlayerUtils {
    var shouldBypassVolume = false

    /**
     * Plays a sound at a specified volume and pitch, bypassing the default volume setting.
     *
     * @param sound The identifier of the sound to be played.
     * @param volume The volume at which the sound should be played.
     * @param pitch The pitch at which the sound should be played.
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
        devMessage("Clicked")
        PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
    }

    fun stopVelocity(){
        mc.thePlayer.setVelocity(0.0, mc.thePlayer.motionY, 0.0)
    }



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

    private var lastClickSent = 0L

    @SubscribeEvent
    fun onPacketSend(event: noobroutes.events.impl.PacketEvent.Send) {
        if (event.packet !is C0EPacketClickWindow) return
        lastClickSent = System.currentTimeMillis()
    }

    fun windowClick(slotId: Int, button: Int, mode: Int) {
        if (lastClickSent + 45 > System.currentTimeMillis())
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
    inline val Vec3.distanceToPlayerSq get() = mc.thePlayer.positionVector.squareDistanceTo(this)
    inline val Vec3.distanceToPlayer get() = mc.thePlayer.positionVector.distanceTo(this)
    inline val BlockPos.distanceToPlayer get() = mc.thePlayer.positionVector.distanceTo(Vec3(this))
    inline val BlockPos.distanceToPlayerSq get() = mc.thePlayer.positionVector.squareDistanceTo(Vec3(this))



    fun unSneak(){
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, Keyboard.isKeyDown(mc.gameSettings.keyBindSneak.keyCode))
    }

    fun sneak(){
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, true)
    }


    inline val keyBindings get() =  listOf(
        mc.gameSettings.keyBindForward,
        mc.gameSettings.keyBindLeft,
        mc.gameSettings.keyBindRight,
        mc.gameSettings.keyBindBack,
        mc.gameSettings.keyBindJump
    )

    inline val keyBindingsKeyCodes get() = keyBindings.map { it.keyCode}

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