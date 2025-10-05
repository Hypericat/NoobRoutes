package noobroutes.features.floor7.autop3.rings

import com.google.gson.JsonObject
import noobroutes.features.floor7.autop3.*
import noobroutes.features.floor7.autop3.WalkBoost.Companion.asWalkBoost
import noobroutes.utils.capitalizeFirst


class WalkRing(
    ringBase: RingBase = RingBase(),
    var walkBoost: WalkBoost = WalkBoost.UNCHANGED
) : Ring(ringBase, RingType.WALK) {
    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {

            val boost = WalkBoost.entries.map { Pair(it.name.lowercase(), it) }
                .firstOrNull { args.any { arg -> arg == it.first } } ?: Pair(
                "Unchanged",
                WalkBoost.UNCHANGED
            )

            return if (args.any { it == "jump" }) JumpRing(generateRingBaseFromArgs(args), true) else WalkRing(
                generateRingBaseFromArgs(args),
                boost.second
            )
        }
    }

    override fun addRingData(obj: JsonObject) {
        obj.addProperty("boost", walkBoost.name)
    }

    override fun loadRingData(obj: JsonObject) {
        super.loadRingData(obj)
        walkBoost = obj.get("boost")?.asWalkBoost() ?: WalkBoost.UNCHANGED

    }




    override fun doRing() {
        super.doRing()
        if (walkBoost != WalkBoost.UNCHANGED) {
            AutoP3.walkBoost = walkBoost.name.lowercase().capitalizeFirst()
        }
        AutoP3MovementHandler.setDirection(yaw)
    }
}