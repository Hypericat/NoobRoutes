package noobroutes.features.floor7.autop3.rings

import com.google.gson.JsonObject
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.*
import noobroutes.features.floor7.autop3.WalkBoost.Companion.asWalkBoost
import noobroutes.ui.editgui.EditGuiBase
import noobroutes.utils.Scheduler
import noobroutes.utils.Utils
import noobroutes.utils.capitalizeFirst
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.modMessage

class HClipRing(
    ringBase: RingBase = RingBase(),
    var walk: Boolean = false,
    var insta: Boolean = false,
    var walkBoost: WalkBoost = WalkBoost.UNCHANGED
) : Ring(ringBase, RingType.H_CLIP) {
    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {
            val insta = args.any {it.lowercase() == "insta"}
            val boost = getWalkBoost(args)

            return HClipRing(generateRingBaseFromArgs(args), getWalkFromArgs(args), insta, boost)
        }
    }

    override fun addRingData(obj: JsonObject) {
        obj.addProperty("walkBoost", walkBoost.name)
    }

    override fun loadRingData(obj: JsonObject) {
        super.loadRingData(obj)
        walkBoost = obj.get("walkBoost")?.asWalkBoost() ?: WalkBoost.UNCHANGED
    }


    init {
        addBoolean("walk", {walk}, {walk = it})
        addBoolean("Insta", {insta}, {insta = it})
    }

    override fun doRing() {
        super.doRing()

        if (walkBoost != WalkBoost.UNCHANGED) {
            AutoP3.walkBoost = walkBoost.name.lowercase().capitalizeFirst()
        }

        if (insta) {
            if (mc.thePlayer.motionX != 0.0 || mc.thePlayer.motionZ != 0.0) return modMessage("must be not moving in x/z")
            setMaxSpeed()
            if (walk) Scheduler.scheduleHighestPostMoveEntityWithHeadingTask { AutoP3MovementHandler.setDirection(yaw) }
            return
        }

        PlayerUtils.stopVelocity()
        Scheduler.scheduleHighestPostMoveEntityWithHeadingTask { setMaxSpeed() }



        if (walk) Scheduler.scheduleHighestPostMoveEntityWithHeadingTask(1) { AutoP3MovementHandler.setDirection(yaw) }
    }

    private fun setMaxSpeed() {
        mc.thePlayer.motionX = AutoP3MovementHandler.DEFAULT_SPEED * PlayerUtils.getPlayerWalkSpeed() * Utils.xPart(yaw)
        mc.thePlayer.motionZ = AutoP3MovementHandler.DEFAULT_SPEED * PlayerUtils.getPlayerWalkSpeed() * Utils.zPart(yaw)
    }

    override fun extraArgs(builder: EditGuiBase.EditGuiBaseBuilder) {
        builder.addSwitch("Insta", {insta}, {insta = it})
        builder.addSwitch("Walk", {walk}, {walk = it})
        builder.addSelector(
            "Walk Boost",
            WalkBoost.getOptionsList(),
            { walkBoost.getIndex() },
            { walkBoost = WalkBoost[it]}
        )
    }
}