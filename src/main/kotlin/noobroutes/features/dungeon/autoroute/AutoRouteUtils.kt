package noobroutes.features.dungeon.autoroute

import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.PacketReturnEvent
import noobroutes.features.move.Zpew
import noobroutes.utils.PacketUtils
import noobroutes.utils.RotationUtils.offset
import noobroutes.utils.floor
import noobroutes.utils.skyblock.PlayerUtils

object AutoRouteUtils {

    var pearlSoundRegistered = false
    var sneakRegistered = false
    var unsneakRegistered = false

    fun ether() {
        sneakRegistered = true
        PlayerUtils.sneak()
    }

    var aotvTarget: BlockPos? = null
    fun aotv(pos: BlockPos) {
        aotvTarget = pos
        unsneakRegistered = true
        PlayerUtils.forceUnSneak()
    }

    var clipDistance = 0
    var clipRegistered = false


    var serverSneak = false
    @SubscribeEvent
    fun onPacketSendReturn(event: PacketReturnEvent.Send) {
        if (event.packet !is C0BPacketEntityAction) return
        serverSneak = when (event.packet.action) {
            C0BPacketEntityAction.Action.START_SNEAKING -> true
            C0BPacketEntityAction.Action.STOP_SNEAKING -> false
            else -> serverSneak
        }
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Receive) {
        if (clipRegistered && event.packet is S08PacketPlayerPosLook) {
            event.isCanceled = true
            PacketUtils.sendPacket(C06PacketPlayerPosLook(event.packet.x, event.packet.y, event.packet.z, event.packet.yaw, event.packet.pitch, false))
            mc.thePlayer.setPosition(
                mc.thePlayer.posX.floor() + 0.5,
                mc.thePlayer.posY.floor() - clipDistance,
                mc.thePlayer.posZ.floor() + 0.5
            )
            pearlSoundRegistered = false
            clipRegistered = false
        }

        if (!pearlSoundRegistered || event.packet !is S29PacketSoundEffect) return
        if (event.packet.soundName != "random.bow" || event.packet.volume != 0.5f) return
        clipRegistered = true
    }

    @SubscribeEvent
    fun unsneak(event: RenderWorldLastEvent) {
        if (!unsneakRegistered) return
        if (mc.thePlayer.isSneaking) PlayerUtils.forceUnSneak()
        if (serverSneak) return
        PlayerUtils.airClick()
        aotvTarget?.let { Zpew.doZeroPingAotv(it) }
        resetRotation()
        unsneakRegistered = false
        PlayerUtils.resyncSneak()
    }

    @SubscribeEvent
    fun sneak(event: RenderWorldLastEvent) {
        if (!sneakRegistered) return
        if (!mc.thePlayer.isSneaking) PlayerUtils.sneak()
        if (!serverSneak) return
        PlayerUtils.airClick()
        resetRotation()
        sneakRegistered = false
        PlayerUtils.resyncSneak()
    }

    fun resetRotation() {
        rotating = false
        rotatingPitch = null
        rotatingYaw = null
    }
    fun setRotation(yaw: Float?, pitch: Float?) {
        rotating = true
        rotatingPitch = pitch
        rotatingYaw = yaw
    }


    var rotating = false
    var rotatingYaw: Float? = null
    var rotatingPitch: Float? = null
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun motion(event: MotionUpdateEvent.Pre) {
        if (rotating) {
            rotatingYaw?.let {
                event.yaw = it + offset
            }
            rotatingPitch?.let {
                event.pitch = it + offset
            }
        }
    }

}