package noobroutes.features.floor7

import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.Blink.skip
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.utils.AutoP3Utils
import noobroutes.utils.AutoP3Utils.walking
import noobroutes.utils.Scheduler
import noobroutes.utils.Utils.isClose
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

object CoreClip: Module(
    name = "CoreClip",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "clips u through the gold blocks into core"
) {

    @SubscribeEvent
    fun onMotionPre(event: MotionUpdateEvent.Pre) {
        if (mc.thePlayer == null) return
        if (event.y != 115.0) return
        if (event.x !in 52.0..57.0) return

        if (isClose(event.z, 53.7)) {
            event.motionZ = 0.0
            event.z = 53.7624
            Scheduler.scheduleC03Task {mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, 55.301)}
        }
        else if (isClose(event.z, 55.3)){
            event.motionZ = 0.0
            event.z = 55.2376
            Scheduler.scheduleC03Task {mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY, 53.699)}
        }
    }
}