package noobroutes.features.floor7.autop3.rings

import com.google.gson.JsonObject
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.features.floor7.autop3.Ring
import noobroutes.utils.AuraManager
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.JsonHelper.addProperty

class BoomRing(
    coords: Vec3,
    yaw: Float,
    term: Boolean,
    leap: Boolean,
    left: Boolean,
    center: Boolean,
    rotate: Boolean,
    val block: BlockPos
) : Ring(listOf("boom", "tnt"), coords, yaw, term, leap, left, center, rotate) {

    override fun addRingData(obj: JsonObject) {
        obj.addProperty("block", block)
    }

    override fun doRing() {
        super.doRing()
        SwapManager.swapFromName("TNT")
        Scheduler.schedulePreTickTask(1) { AuraManager.auraBlock(block, force = true) }
    }
}