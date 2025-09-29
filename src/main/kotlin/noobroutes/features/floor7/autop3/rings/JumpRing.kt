package noobroutes.features.floor7.autop3.rings

import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.*
import noobroutes.ui.editgui.EditGuiBase
import noobroutes.utils.Scheduler
import noobroutes.utils.skyblock.LowHopUtils
import noobroutes.utils.skyblock.modMessage


class JumpRing (
    ringBase: RingBase = RingBase(),
    var walk: Boolean = false,
    var lowHop: Boolean = false,
    var boost: Boolean = false
) : Ring(ringBase, RingType.JUMP) {
    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {
            val boostPresent = args.any {it.lowercase() == "boost"}
            return JumpRing(generateRingBaseFromArgs(args), getWalkFromArgs(args), getLowFromArgs(args), boostPresent)
        }
    }

    init {
        addBoolean("walk", {walk}, {walk = it})
        addBoolean("low", {lowHop}, {lowHop = it})
        addBoolean("boost", {boost}, {boost = it})
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
    }
}