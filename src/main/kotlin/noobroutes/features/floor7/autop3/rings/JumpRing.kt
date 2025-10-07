package noobroutes.features.floor7.autop3.rings

import com.google.gson.JsonObject
import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.*
import noobroutes.features.floor7.autop3.WalkBoost.Companion.asWalkBoost
import noobroutes.ui.editgui.EditGuiBase
import noobroutes.utils.Scheduler
import noobroutes.utils.capitalizeFirst
import noobroutes.utils.skyblock.LowHopUtils
import noobroutes.utils.skyblock.modMessage


class JumpRing (
    ringBase: RingBase = RingBase(),
    var walk: Boolean = false,
    var lowHop: Boolean = false,
    var boost: Boolean = false,
    var walkBoost: WalkBoost = WalkBoost.UNCHANGED
) : Ring(ringBase, RingType.JUMP) {
    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {
            val boostPresent = args.any {it.lowercase() == "boost"}
            val boost = getWalkBoost(args)

            return JumpRing(generateRingBaseFromArgs(args), getWalkFromArgs(args), getLowFromArgs(args), boostPresent, boost)
        }
    }

    init {
        addBoolean("walk", {walk}, {walk = it})
        addBoolean("low", {lowHop}, {lowHop = it})
        addBoolean("boost", {boost}, {boost = it})
    }


    override fun addRingData(obj: JsonObject) {
        obj.addProperty("walkBoost", walkBoost.name)
    }

    override fun loadRingData(obj: JsonObject) {
        super.loadRingData(obj)
        walkBoost = obj.get("walkBoost")?.asWalkBoost() ?: WalkBoost.UNCHANGED
    }


    override fun doRing() {
        super.doRing()

        if (!mc.thePlayer.onGround) {
            triggered = false
            return
        }

        if (lowHop) {
            if (!LowHopUtils.disabled) return modMessage("Cant cause not disabled")
            Scheduler.scheduleHighestPostMoveEntityWithHeadingTask { LowHopUtils.lowHopThisJump = true }
        }
        mc.thePlayer.jump()

        if (boost) AutoP3MovementHandler.boost2ndTick = true

        if (walkBoost != WalkBoost.UNCHANGED) {
            AutoP3.walkBoost = walkBoost.name.lowercase().capitalizeFirst()
        }
        if (walk) {
            AutoP3MovementHandler.setDirection(yaw)
            AutoP3MovementHandler.setVelocity(AutoP3MovementHandler.DEFAULT_SPEED)
            AutoP3MovementHandler.setJumpingTrue()
        }
    }

    override fun extraArgs(builder: EditGuiBase.EditGuiBaseBuilder) {
        builder.addSwitch("Walk", {walk}, {walk = it})
        builder.addSwitch("LowHop", {lowHop}, {lowHop = it})
        builder.addSwitch("Boost", {boost}, {boost = it})
        builder.addSelector(
            "Walk Boost",
            WalkBoost.getOptionsList(),
            { walkBoost.getIndex() },
            { walkBoost = WalkBoost[it]}
        )
    }
}