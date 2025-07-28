package noobroutes.features.floor7.autop3.rings

import noobroutes.features.floor7.autop3.CommandGenerated
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingBase
import noobroutes.features.floor7.autop3.RingType


class SpedRing(
    ringBase: RingBase,
    var length: Int = 0
) : Ring(ringBase, RingType.SPEED) {

    init {
        addInt("length", {length}, {length = it})
    }

    override fun doRing() {
        /*super.doRing()

        if (length > cancelled || blinksInstance + length > AutoP3.maxBlinks || !speedRings) return
        if (length < 1.0) {
            modMessage("Broken Speed Ring, cancelling execution")
            return
        }
        blinksInstance += length
        Blink.cancelled -= length

        repeat(length - 1) {

            AutoP3RingEvent().postAndCatch()
            AutoP3MovementEvent().postAndCatch()

            mc.thePlayer.moveEntityWithHeading(0f, 0f)

            PacketUtils.sendPacket(C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.onGround))

            if (abs(mc.thePlayer.motionY) < 0.004) mc.thePlayer.motionY = 0.0 // fuck this fucking shit ass game it should fucking kys
            if (abs(mc.thePlayer.motionX) < 0.004 && !AutoP3Utils.walking && !AutoP3Utils.motioning) mc.thePlayer.motionX = 0.0
            if (abs(mc.thePlayer.motionZ) < 0.004 && !AutoP3Utils.walking && !AutoP3Utils.motioning) mc.thePlayer.motionZ = 0.0

            //AutoP3MovementEvent().postAndCatch()

        }
        PacketUtils.sendPacket(C03PacketPlayer.C04PacketPlayerPosition(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, mc.thePlayer.onGround))

        AutoP3RingEvent().postAndCatch()

        //AutoP3MovementEvent().postAndCatch()

        if (AutoP3.renderStyle == 3) modMessage("Blinking", "§0[§6Yharim§0]§7 ")
        else modMessage("§c§l${cancelled}§r§f c04s available, used §c${length}§f,  §7(${AutoP3.maxBlinks - blinksInstance} left on this instance)")
        */
    }


}