package noobroutes.features.floor7.autop3.rings

import noobroutes.Core.mc
import noobroutes.features.floor7.autop3.*
import noobroutes.ui.editgui.EditGuiBase


class JumpRing (
    ringBase: RingBase = RingBase(),
    var walk: Boolean = false,
) : Ring(ringBase, RingType.JUMP) {
    companion object : CommandGenerated {
        override fun generateRing(args: Array<out String>): Ring? {
            return JumpRing(generateRingBaseFromArgs(args), getWalkFromArgs(args))
        }
    }

    init {
        addBoolean("walk", {walk}, {walk = it})
    }

    override fun doRing() {
        super.doRing()

        if (mc.thePlayer.onGround) mc.thePlayer.jump()
        else {
            triggered = false
            return
        }

        if (walk) {
            AutoP3MovementHandler.setDirection(yaw)
            AutoP3MovementHandler.setVelocity(AutoP3MovementHandler.DEFAULT_SPEED)
            AutoP3MovementHandler.setJumpingTrue()
        }
    }
    override fun extraArgs(builder: EditGuiBase.EditGuiBaseBuilder) {
        builder.addSwitch("Walk", {walk}, {walk = it})
    }
}