package noobroutes.utils


import noobroutes.Core.mc
import noobroutes.events.impl.S08Event
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object Etherwarper {
    var warping = false

    private var sendCoords = Vec3(0.0, 0.0, 0.0)
    fun etherwarpToVec3(vec3: Vec3, silent: Boolean = false){
        if (sendCoords.distanceTo(mc.thePlayer.positionVector) < 0.2) return
        devMessage("sendcoords: $sendCoords, positionVec: ${mc.thePlayer.positionVector}, vec3: $vec3")
        val state = SwapManager.swapFromSBId("ASPECT_OF_THE_END", "ASPECT_OF_THE_VOID")
        if (state == SwapManager.SwapState.UNKNOWN || state == SwapManager.SwapState.TOO_FAST) return
        sendCoords = mc.thePlayer.positionVector
        PlayerUtils.unPressKeys()
        PlayerUtils.sneak()
        warping = true
        mc.thePlayer.setVelocity(0.0, mc.thePlayer.motionY, 0.0)
        val rot = RotationUtils.getYawAndPitch(vec3, true)
        RotationUtils.clickAt(rot.first, rot.second, silent, true)



    }


    @SubscribeEvent
    fun onS08(event: S08Event) {
        warping = false
    }
}