package com.github.wadey3636.noobroutes.utils

import com.github.wadey3636.noobroutes.Core.mc
import net.minecraftforge.client.event.RenderPlayerEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent

object SilentRotator {
    class RealRotation(var yaw: Float?, var pitch: Float?, var prevYaw: Float?, var prevPitch: Float?)
    class RenderPitch(var now: Float, var prev: Float, var realNow: Float, var realPrev: Float)
    var blockSilentRotation = false
    var pitchRenderer0Registered = false
    var pitchRenderer1Registered = false

    private var realRotations: RealRotation = RealRotation(null, null, null, null)
    private var renderPitch: RenderPitch = RenderPitch(0f, 0f, 0f, 0f)

    fun doSilentRotation() {
        if (blockSilentRotation || mc.thePlayer == null) return
        if (realRotations.yaw != null &&
            realRotations.pitch != null &&
            realRotations.prevYaw != null &&
            realRotations.prevPitch != null
            ) return
        realRotations.yaw = mc.thePlayer.rotationYaw
        realRotations.pitch = mc.thePlayer.rotationPitch
        realRotations.prevYaw = mc.thePlayer.prevRotationYaw
        realRotations.prevPitch = mc.thePlayer.prevRotationPitch

        Scheduler.scheduleHighPostTickTask {
            registerRender()
            mc.thePlayer.rotationYaw = realRotations.yaw!!
            mc.thePlayer.rotationPitch = realRotations.pitch!!
            mc.thePlayer.prevRotationYaw = realRotations.prevYaw!!
            mc.thePlayer.prevRotationPitch = realRotations.prevPitch!!

            realRotations.yaw = null
            realRotations.pitch = null
            realRotations.prevYaw = null
            realRotations.prevPitch = null

            Scheduler.scheduleHighPreTickTask {
                unregisterRender()
            }
        }

    }




    fun registerRender() {
        if (mc.thePlayer == null) return
        renderPitch.now = mc.thePlayer.rotationPitch
        renderPitch.prev = mc.thePlayer.prevRotationPitch
        mc.thePlayer.renderYawOffset = mc.thePlayer.rotationYaw
        mc.thePlayer.rotationYawHead = mc.thePlayer.rotationYaw
        pitchRenderer0Registered = true
        pitchRenderer1Registered = true
    }


    fun unregisterRender() {
        pitchRenderer0Registered = false
        pitchRenderer1Registered = false
    }

    @SubscribeEvent
    fun pitchRenderer0(event: RenderPlayerEvent.Pre) {
        if (!pitchRenderer0Registered) return
        val thePlayer = event.entity ?: return
        if (mc.thePlayer == null || thePlayer != mc.thePlayer) return
        renderPitch.now = thePlayer.rotationPitch
        renderPitch.prev = thePlayer.prevRotationPitch
        thePlayer.rotationPitch = renderPitch.realNow
        thePlayer.prevRotationPitch = renderPitch.realPrev
    }

    @SubscribeEvent
    fun pitchRenderer1(event: RenderPlayerEvent.Post) {
        if (!pitchRenderer1Registered) return
        val thePlayer = event.entity ?: return
        if (mc.thePlayer == null || thePlayer != mc.thePlayer) return
        thePlayer.rotationPitch = renderPitch.realNow
        thePlayer.prevRotationPitch = renderPitch.realPrev
    }



}