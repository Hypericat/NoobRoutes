package noobroutes.utils

import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core.mc
import noobroutes.events.impl.PacketReturnEvent
import noobroutes.events.impl.ServerTickEvent
import noobroutes.utils.render.RenderUtils.renderVec
import noobroutes.utils.skyblock.EtherWarpHelper
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import org.lwjgl.input.Keyboard


object Etherwarper {
    var warping = false
    private var currentEWTarget: Vec3? = null


    var prevEWLocation: Vec3? = null

    fun etherwarpToVec3(vec3: Vec3, silent: Boolean = false) {
        val rot = RotationUtils.getYawAndPitch(vec3)
        etherwarp(rot.first, rot.second, silent)
    }

    fun etherwarp(yaw: Float, pitch: Float, silent: Boolean = false){
        if (warping) return
        prevEWLocation?.let {
            if (it.distanceTo(mc.thePlayer.positionVector) < 0.4) return
        }
        warping = true
        setEWTarget(yaw, pitch)
        PlayerUtils.sneak()
        PlayerUtils.stopVelocity()
        val state = SwapManager.swapFromSBId("ASPECT_OF_THE_VOID")
        when (state) {
            SwapManager.SwapState.ALREADY_HELD -> {
                prevEWLocation = mc.thePlayer.positionVector
                RotationUtils.rotate(yaw, pitch, silent, RotationUtils.Action.RightClick)
            }
            SwapManager.SwapState.SWAPPED -> {
                prevEWLocation = mc.thePlayer.positionVector
                Scheduler.schedulePreTickTask {
                    RotationUtils.rotate(yaw, pitch, silent, RotationUtils.Action.RightClick)
                }
            }
            else -> return
        }
    }

    fun preRotateEtherwarpToVec3(vec3: Vec3, silent: Boolean = false, noWarpingCheck: Boolean = false){
        val rot = RotationUtils.getYawAndPitch(vec3)
        preRotateEtherwarp(rot.first, rot.second, silent, noWarpingCheck)
        //devMessage(vec3)
    }

    fun preRotateEtherwarp(yaw: Float, pitch: Float, silent: Boolean = false,  noWarpingCheck: Boolean = false) {
        devMessage("$warping|$noWarpingCheck")
        if ((warping && !noWarpingCheck) || PlayerUtils.playerControlsKeycodes.any { Keyboard.isKeyDown(it)}) return
        prevEWLocation?.let {
            if (it.distanceTo(mc.thePlayer.positionVector) < 0.4) return
        }
        warping = true
        val etherwarp = EtherWarpHelper.getEtherPos(PositionLook(mc.thePlayer.renderVec, yaw, pitch))
        if (!etherwarp.succeeded || etherwarp.vec == null) {
            devMessage("Failed to get etherwarp")
            return
        }
        devMessage("ew target")
        setEWTarget(yaw, pitch)

        PlayerUtils.sneak()
        PlayerUtils.stopVelocity()
        val state = SwapManager.swapFromSBId("ASPECT_OF_THE_VOID")
        when (state) {
            SwapManager.SwapState.ALREADY_HELD -> {
                prevEWLocation = mc.thePlayer.positionVector
                RotationUtils.rotate(yaw, pitch, silent, RotationUtils.Action.RightClick, continuous = RotationUtils.CompletionRequirement.PreRotate)
            }
            SwapManager.SwapState.SWAPPED -> {
                prevEWLocation = mc.thePlayer.positionVector
                Scheduler.schedulePreTickTask {
                    RotationUtils.rotate(yaw, pitch, silent, RotationUtils.Action.RightClick, continuous = RotationUtils.CompletionRequirement.PreRotate)
                }
            }
            else -> return
        }
    }

    fun doubleTickEtherwarp(yaw: Float, pitch: Float, silent: Boolean){
        if (warping) return
        prevEWLocation?.let {
            if (it.distanceTo(mc.thePlayer.positionVector) < 0.4) return
        }
        warping = true
        PlayerUtils.sneak()
        PlayerUtils.stopVelocity()
        setEWTarget(yaw, pitch)
        val state = SwapManager.swapFromSBId("ASPECT_OF_THE_VOID")
        if (state == SwapManager.SwapState.ALREADY_HELD || state == SwapManager.SwapState.SWAPPED) {
            prevEWLocation = mc.thePlayer.positionVector
            RotationUtils.rotate(yaw, pitch, silent)
            Scheduler.schedulePreTickTask(1) {
                RotationUtils.rotate(yaw, pitch, silent, RotationUtils.Action.RightClick)
            }
        }
    }

    private fun setEWTarget(yaw: Float, pitch: Float) {
        val etherwarp = EtherWarpHelper.getEtherPos(PositionLook(mc.thePlayer.renderVec, yaw, pitch))
        if (!etherwarp.succeeded || etherwarp.vec == null) {
            devMessage("Failed to get etherwarp")
            return
        }
        currentEWTarget = etherwarp.vec
    }


    private var serverTicks = 0L
    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent){
        if (serverTicks >= 4) {
            warping = false
            serverTicks = 0
        }
        if (warping && RotationUtils.ticksRotated == 0L) {
            devMessage("tick")
            serverTicks++
        }
    }

    @SubscribeEvent
    fun onPacketSendReturn(event: PacketReturnEvent.Send){
        if (event.packet !is C06PacketPlayerPosLook) return
        if ((currentEWTarget?.add(0.5, 1.0, 0.5)?.distanceTo(Vec3(event.packet.positionX, event.packet.positionY, event.packet.positionZ)) ?: return) < 0.1) {
            warping = false
            devMessage("Completed Etherwarp")
            currentEWTarget = null

        }
    }
    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (!warping && (prevEWLocation?.distanceTo(mc.thePlayer.positionVector) ?: return) > 1)  prevEWLocation = null
    }


    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Unload) {
        warping = false
    }


    @SubscribeEvent
    fun onInput(event: InputEvent.KeyInputEvent) {
        if (!warping) return
        val keycode = Keyboard.getEventKey()
        if (keycode == mc.gameSettings.keyBindSneak.keyCode) {
            PlayerUtils.sneak()
        }
    }



}