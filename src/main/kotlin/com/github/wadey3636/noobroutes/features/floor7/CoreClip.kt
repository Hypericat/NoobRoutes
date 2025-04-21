package com.github.wadey3636.noobroutes.features.floor7

import com.github.wadey3636.noobroutes.features.Blink.skip
import com.github.wadey3636.noobroutes.utils.AutoP3Utils
import com.github.wadey3636.noobroutes.utils.AutoP3Utils.walking
import com.github.wadey3636.noobroutes.utils.PacketUtils
import com.github.wadey3636.noobroutes.utils.Utils.isClose
import me.modcore.features.Category
import me.modcore.features.Module
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.lwjgl.input.Keyboard

object CoreClip: Module(
    name = "CoreClip",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "clips u through the gold blocks into core"
) {

    private var cd = 0
    private var doWalk = false

    @SubscribeEvent
    fun atCore(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (mc.thePlayer == null) return
        if (cd > 0) {
            cd--
            return
        }
        if (mc.thePlayer.posY != 115.0) return
        if (mc.thePlayer.posX !in 52.0..57.0) return

        if (isClose(mc.thePlayer.posZ, 53.7)) doClip(53.7624, 55.301)
        else if (isClose(mc.thePlayer.posZ, 55.3)) doClip(55.2376, 53.699)
    }

    private fun doClip(coord1: Double, coord2: Double) {
        doWalk = walking
        AutoP3Utils.unPressKeys()
        mc.thePlayer.motionZ = 0.0
        cd = 3
        mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, coord1)
        skip = true
        PacketUtils.c03ScheduleTask {mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, coord2)}
        PacketUtils.c03ScheduleTask(1) {
            walking = doWalk
            AutoP3Utils.rePressKeys()
            skip = false
        }
    }
}