package noobroutes.features.floor7.autop3.rings

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.AutoP3Utils
import noobroutes.utils.AutoP3Utils.awaitingTick
import noobroutes.utils.AutoP3Utils.direction
import noobroutes.utils.AutoP3Utils.walkAfter
import noobroutes.utils.AutoP3Utils.walking
import noobroutes.utils.Scheduler
import noobroutes.utils.Utils
import noobroutes.utils.skyblock.modMessage
@RingType("HClip")
class HClipRing(
    coords: Vec3 = Vec3(0.0, 0.0, 0.0),
    yaw: Float = 0f,
    term: Boolean = false,
    leap: Boolean = false,
    left: Boolean = false,
    center: Boolean = false,
    rotate: Boolean = false,
    var walk: Boolean = false
) : Ring(coords, yaw, term, leap, left, center, rotate) {

    init {
        addBoolean("walk", {walk}, {walk = it})
    }

    override fun doRing() {
        if (mc.thePlayer.onGround) {
            modMessage("use jump or yeet, not hclip")
            return
        }
        AutoP3Utils.unPressKeys()
        super.doRing()
        mc.thePlayer.motionX = 0.0
        mc.thePlayer.motionZ = 0.0
        AutoP3Utils.direction = yaw
        Scheduler.schedulePreTickTask(1) {
            val speed = mc.thePlayer.capabilities.walkSpeed * 2.806
            mc.thePlayer.motionX = speed * Utils.xPart(direction)
            mc.thePlayer.motionZ = speed * Utils.zPart(direction)
        }
        if (walk) Scheduler.schedulePreTickTask(1) { walking = true }
    }
}