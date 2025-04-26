package com.github.wadey3636.noobroutes.features.floor7

import com.github.wadey3636.noobroutes.features.Blink.skip
import com.github.wadey3636.noobroutes.utils.AutoP3Utils
import com.github.wadey3636.noobroutes.utils.AutoP3Utils.walking
import com.github.wadey3636.noobroutes.utils.Scheduler
import com.github.wadey3636.noobroutes.utils.Utils.isClose
import com.github.wadey3636.noobroutes.events.impl.MotionUpdateEvent
import com.github.wadey3636.noobroutes.features.Category
import com.github.wadey3636.noobroutes.features.Module
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
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
    fun onMotionPre(event: MotionUpdateEvent.Pre) {
        if (mc.thePlayer == null) return
        if (cd > 0) {
            cd--
            return
        }
        if (event.y != 115.0) return
        if (event.x !in 52.0..57.0) return

        if (isClose(event.z, 53.7)) {
            event.motionZ = 0.0
            event.z = 53.7624
            doClip(55.301)
        }
        else if (isClose(event.z, 55.3)){
            event.motionZ = 0.0
            event.z = 55.2376
            doClip(53.699)
        }
    }

    private fun doClip(coord: Double) {
        doWalk = walking
        AutoP3Utils.unPressKeys()
        cd = 3
        skip = true
        Scheduler.scheduleC03Task {mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, coord)}
        Scheduler.scheduleC03Task(1) {
            walking = doWalk
            AutoP3Utils.rePressKeys()
            skip = false
        }
    }
}