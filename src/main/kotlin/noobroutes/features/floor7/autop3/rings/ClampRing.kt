package noobroutes.features.floor7.autop3.rings

import com.google.gson.JsonObject
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.*
import noobroutes.features.floor7.autop3.WalkBoost.Companion.asWalkBoost
import noobroutes.ui.editgui.EditGuiBase
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart
import noobroutes.utils.capitalizeFirst


class ClampRing(
    ringBase: RingBase = RingBase(),
    var walk: Boolean = false,
    var walkBoost: WalkBoost = WalkBoost.UNCHANGED
) : Ring(ringBase, RingType.CLAMP) {
    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {
            val boost = getWalkBoost(args)

            return ClampRing(generateRingBaseFromArgs(args), getWalkFromArgs(args), boost)
        }
    }

    init {
        addBoolean("walk", {walk}, {walk = it})
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
        if (walk) AutoP3MovementHandler.setDirection(yaw)

        val motionX = mc.thePlayer.motionX
        val motionZ = mc.thePlayer.motionZ
        if (motionX * xPart(yaw) < 0 || motionZ * zPart(yaw) < 0) {
            mc.thePlayer.motionX = 0.0
            mc.thePlayer.motionZ = 0.0
            return
        }
        val scaleX = if (xPart(yaw) != 0.0) motionX / xPart(yaw) else Double.POSITIVE_INFINITY
        val scaleZ = if (zPart(yaw) != 0.0) motionZ / zPart(yaw) else Double.POSITIVE_INFINITY
        val scale = minOf(scaleX, scaleZ)
        mc.thePlayer.motionX = xPart(yaw) * scale
        mc.thePlayer.motionZ = zPart(yaw) * scale
    }
    override fun extraArgs(builder: EditGuiBase.EditGuiBaseBuilder) {
        builder.addSwitch("Walk", {walk}, {walk = it})
        builder.addSelector(
            "Walk Boost",
            WalkBoost.getOptionsList(),
            { walkBoost.getIndex() },
            { walkBoost = WalkBoost[it]}
        )
    }
}