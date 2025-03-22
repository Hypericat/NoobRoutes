package com.github.wadey3636.noobroutes.features

import com.github.wadey3636.noobroutes.utils.AutoP3Utils
import io.github.moulberry.notenoughupdates.util.roundToDecimals
import me.defnotstolen.events.impl.PacketEvent
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard
import kotlin.math.abs
import kotlin.math.roundToInt

object CoreClip : Module(
    name = "CoreClip",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "clips u through the gold blocks into core"
) {
    private var clipping: String? = null

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (mc.thePlayer == null) return
        if (mc.thePlayer.posY != 115.0) return
        if (mc.thePlayer.posX !in 52.0..57.0) return

        if (!isClose(mc.thePlayer.posZ, 53.7) && !isClose(mc.thePlayer.posZ, 55.3)) return

        if (isClose(mc.thePlayer.posZ, 53.7)) {
            mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, 53.7624)
            clipping = "in"
        }
        else {
            mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, 53.2376)
            clipping = "out"
        }

        mc.thePlayer.motionX = 0.0
        mc.thePlayer.motionZ = 0.0
        AutoP3Utils.unPressKeys()
    }

    @SubscribeEvent
    fun onC03(event: PacketEvent.Send) {
        if (event.packet !is C03PacketPlayer || clipping == null) return
        if (clipping == "in") mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, 55.301)
        else mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, 53.699)
        clipping = null
        AutoP3Utils.rePressKeys()
    }

    private fun isClose(number1: Double, number2: Double): Boolean {
        return abs(number1 - number2) < 0.0001F
    }
}