package noobroutes.utils


import noobroutes.Core.mc
import noobroutes.events.impl.S08Event
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.utils.RotationUtils.canSendC08
import noobroutes.utils.RotationUtils.queuedRots
import noobroutes.utils.Utils.isStart
import noobroutes.utils.render.RenderUtils.renderVec
import noobroutes.utils.skyblock.EtherWarpHelper
import org.lwjgl.input.Keyboard

object Etherwarper {
    class EtherwarpTarget(val target: Vec3, val silent: Boolean)
    var warping = false
    private val etherwarpTargets = mutableListOf<EtherwarpTarget>()

    private var sendCoords: Vec3? = null
    private var targetBlock: EtherWarpHelper.EtherPos = EtherWarpHelper.EtherPos(false, null)

    fun etherwarpToVec3(vec3: Vec3, silent: Boolean = false){
        if (queuedRots.isEmpty() && canSendC08) performEtherwarp(vec3, silent)
        else queueEtherwarpToVec3(vec3, silent)

    }
    @SubscribeEvent
    fun onKeyInputEvent(event: InputEvent.KeyInputEvent){
        if (Keyboard.getEventKey() == mc.gameSettings.keyBindSneak.keyCode && warping) {
            PlayerUtils.sneak()
        }
    }

    fun queueEtherwarpToVec3(vec3: Vec3, silent: Boolean = false){
        etherwarpTargets.add(EtherwarpTarget(vec3, silent))
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onTickEvent(event: TickEvent.ClientTickEvent){
        if (!event.isStart || mc.thePlayer == null) return
        sendCoords?.let {
            if (it.distanceTo(mc.thePlayer.positionVector) >= 0.2) sendCoords = null
        }
        if (!warping && etherwarpTargets.isNotEmpty()) {
            devMessage("perform etherwarp queue")
            val ew = etherwarpTargets.removeFirst()
            performEtherwarp(ew.target, ew.silent)
        }
        val target = targetBlock.pos ?: return
        if (targetBlock.succeeded && !isOnBlock(target)) return
        warping = false
        targetBlock = EtherWarpHelper.EtherPos(false, null)
    }

    private fun performEtherwarp(vec3: Vec3, silent: Boolean = false) {
        sendCoords?.let {
            if (it.distanceTo(mc.thePlayer.positionVector) < 0.2) return
        }
        devMessage("sendcoords: $sendCoords, positionVec: ${mc.thePlayer.positionVector}, vec3: $vec3")
        val state = SwapManager.swapFromSBId("ASPECT_OF_THE_END", "ASPECT_OF_THE_VOID")
        if (state == SwapManager.SwapState.UNKNOWN || state == SwapManager.SwapState.TOO_FAST) return
        sendCoords = mc.thePlayer.positionVector
        PlayerUtils.unPressKeys()
        PlayerUtils.sneak()
        warping = true
        mc.thePlayer.setVelocity(0.0, mc.thePlayer.motionY, 0.0)
        val rot = RotationUtils.getYawAndPitch(vec3, true)
        targetBlock = EtherWarpHelper.getEtherPos(mc.thePlayer.renderVec, rot.first, rot.second)
        RotationUtils.clickAt(rot.first, rot.second, silent)
    }



    @SubscribeEvent
    fun onS08(event: S08Event) {
        warping = false
    }
}