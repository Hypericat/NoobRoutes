package noobroutes.features.floor7.autop3.rings

import com.google.gson.JsonObject
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType
import noobroutes.utils.AuraManager
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.json.JsonUtils.addProperty

@RingType("Boom")
class BoomRing(
    coords: Vec3 = Vec3(0.0, 0.0, 0.0),
    yaw: Float = 0f,
    term: Boolean = false,
    leap: Boolean = false,
    left: Boolean = false,
    center: Boolean = false,
    rotate: Boolean = false,
    var block: BlockPos = BlockPos(0, 0, 0),
) : Ring(coords, yaw, term, leap, left, center, rotate) {

    init {
        addBlockPos("block", {block}, {block = it})
    }

    override fun doRing() {
        super.doRing()
        SwapManager.swapFromName("TNT")
        Scheduler.schedulePreTickTask(1) { AuraManager.auraBlock(block, force = true) }
    }
}