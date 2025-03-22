package com.github.wadey3636.noobroutes.features

import com.github.wadey3636.noobroutes.utils.AutoP3Utils
import com.github.wadey3636.noobroutes.utils.Utils
import me.defnotstolen.events.impl.PacketEvent
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.features.settings.impl.NumberSetting
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.lwjgl.input.Keyboard

object HClip: Module(
    name = "HClip",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "hclips when activating"
) {

    private var goNext = false

    override fun onKeybind() {
        toggle()
    }

    override fun onEnable() {
        super.onEnable()
        if (mc.thePlayer == null) return
        if (goNext) return
        mc.thePlayer.motionX = 0.0
        mc.thePlayer.motionZ = 0.0
        AutoP3Utils.unPressKeys()
        goNext = true
    }

    @SubscribeEvent
    fun onC03(event: PacketEvent.Send) {
        if (!goNext) return
        if (event.packet !is C03PacketPlayer) return
        goNext = false
        val speed = mc.thePlayer.capabilities.walkSpeed * 2.806
        mc.thePlayer.motionX = speed * Utils.xPart(mc.thePlayer.rotationYaw)
        mc.thePlayer.motionZ = speed * Utils.zPart(mc.thePlayer.rotationYaw)
        AutoP3Utils.rePressKeys()
        toggle()
        onDisable()
    }
}