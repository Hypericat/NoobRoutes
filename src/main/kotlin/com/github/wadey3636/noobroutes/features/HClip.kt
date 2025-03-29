package com.github.wadey3636.noobroutes.features

import com.github.wadey3636.noobroutes.utils.AutoP3Utils
import com.github.wadey3636.noobroutes.utils.PacketUtils
import com.github.wadey3636.noobroutes.utils.Utils
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.features.settings.Setting.Companion.withDependency
import me.defnotstolen.features.settings.impl.BooleanSetting
import me.defnotstolen.features.settings.impl.NumberSetting
import me.defnotstolen.utils.skyblock.modMessage
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.lwjgl.input.Keyboard
import kotlin.math.atan2

object HClip: Module(
    name = "HClip",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "hclips when activating"
) {
    private val omni by BooleanSetting("omni", false, description = "should go in a direction based of key inputs")
    private val shouldSpam by BooleanSetting("should repeat", false, description = "should repeatedly hclip if holding the button")
    private val hclipInterval by NumberSetting(name = "delay", description = "how long to wait between hclips", min = 2, max = 10, default = 6).withDependency { shouldSpam }

    private var since = 0

    override fun onKeybind() {
        toggle()
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (mc.currentScreen != null) {
            modMessage("Gui Open")
            toggle()
        }
        if (!shouldSpam) return
        if (event.phase != TickEvent.Phase.START) return
        since++
        if (since == hclipInterval) {
            since = 0
            hclip()
        }
    }

    @SubscribeEvent
    fun onKey(event: InputEvent.KeyInputEvent) {
        val key = Keyboard.getEventKey()
        if (key != this.keybinding?.key) return
        if (!Keyboard.getEventKeyState()) {
            since = 0
            toggle()
        }
    }

    private fun hclip() {
        if (mc.thePlayer == null) return
        mc.thePlayer.motionX = 0.0
        mc.thePlayer.motionZ = 0.0
        AutoP3Utils.unPressKeys()
        PacketUtils.c03ScheduleTask {
            val speed = mc.thePlayer.capabilities.walkSpeed * 2.806
            mc.thePlayer.motionX = speed * Utils.xPart(mc.thePlayer.rotationYaw + yawChange())
            mc.thePlayer.motionZ = speed * Utils.zPart(mc.thePlayer.rotationYaw + yawChange())
            AutoP3Utils.rePressKeys()
        }
        if (!shouldSpam) toggle()
    }

    override fun onEnable() {
        super.onEnable()
        hclip()
    }

    private fun yawChange(): Int {
        if (!omni) return 0
        val deltaX = (if (Keyboard.isKeyDown(mc.gameSettings.keyBindRight.keyCode)) 1 else 0) + (if (Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.keyCode)) -1 else 0)
        val deltaZ = (if (Keyboard.isKeyDown(mc.gameSettings.keyBindForward.keyCode)) 1 else 0) + (if (Keyboard.isKeyDown(mc.gameSettings.keyBindBack.keyCode)) -1 else 0)

        if (deltaX == 0 && deltaZ == 0) return 0

        return Math.toDegrees(atan2(deltaX.toDouble(), deltaZ.toDouble())).toInt()
    }


}