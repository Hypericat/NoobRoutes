package noobroutes.features.floor7.autop3.rings

import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType


@RingType("Gay")
class GayRing(
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
        //bha ..asdasdalsjkndalsjknd


    }




}