package com.github.wadey3636.noobroutes.features

import com.github.wadey3636.noobroutes.utils.AutoP3Utils
import com.github.wadey3636.noobroutes.utils.ClientUtils
import com.github.wadey3636.noobroutes.utils.PacketUtils
import io.github.moulberry.notenoughupdates.util.roundToDecimals
import me.defnotstolen.events.impl.PacketEvent
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.lwjgl.input.Keyboard
import kotlin.math.abs
import kotlin.math.roundToInt

object CoreClip : Module(
    name = "CoreClip",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "clips u through the gold blocks into core"
) {

    @SubscribeEvent
    fun atCore(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (mc.thePlayer == null) return
        if (mc.thePlayer.posY != 115.0) return
        if (mc.thePlayer.posX !in 52.0..57.0) return

        if (isClose(mc.thePlayer.posZ, 53.7)) {
            AutoP3Utils.unPressKeys(false)
            mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, 53.7624)
            PacketUtils.c03ScheduleTask {
                mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, 55.301)
                AutoP3Utils.rePressKeys()
            }
        }
        else if (isClose(mc.thePlayer.posZ, 55.3)) {
            AutoP3Utils.unPressKeys()
            mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, 55.2376)
            PacketUtils.c03ScheduleTask {
                mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, 53.699)
                AutoP3Utils.rePressKeys()
            }
        }
    }

    private fun isClose(number1: Double, number2: Double): Boolean {
        return abs(number1 - number2) < 0.0001F
    }
}