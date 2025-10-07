package noobroutes.features.floor7.autop3.rings

import com.google.gson.JsonObject
import noobroutes.features.floor7.autop3.*
import noobroutes.features.floor7.autop3.WalkBoost.Companion.asWalkBoost
import noobroutes.ui.editgui.EditGuiBase
import noobroutes.utils.capitalizeFirst


class WalkRing(
    ringBase: RingBase = RingBase(),
    var walkBoost: WalkBoost = WalkBoost.UNCHANGED
) : Ring(ringBase, RingType.WALK) {
    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {

            val boost = getWalkBoost(args)

            return if (args.any { it == "jump" }) JumpRing(generateRingBaseFromArgs(args), true) else WalkRing(
                generateRingBaseFromArgs(args),
                boost
            )
        }
    }

    override fun addRingData(obj: JsonObject) {
        obj.addProperty("walkBoost", walkBoost.name)
    }

    override fun loadRingData(obj: JsonObject) {
        super.loadRingData(obj)
        walkBoost = obj.get("walkBoost")?.asWalkBoost() ?: WalkBoost.UNCHANGED
    }

    override fun extraArgs(builder: EditGuiBase.EditGuiBaseBuilder) {
        builder.addSelector(
            "Walk Boost",
            WalkBoost.getOptionsList(),
            { walkBoost.getIndex() },
            { walkBoost = WalkBoost[it]}
        )
    }


    override fun doRing() {
        super.doRing()
        if (walkBoost != WalkBoost.UNCHANGED) {
            AutoP3.walkBoost = walkBoost.name.lowercase().capitalizeFirst()
        }
        AutoP3MovementHandler.setDirection(yaw)
    }
}