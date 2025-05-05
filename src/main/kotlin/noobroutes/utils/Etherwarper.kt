package noobroutes.utils

import net.minecraft.client.settings.KeyBinding
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import noobroutes.Core.mc
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.S08Event
import noobroutes.utils.skyblock.PlayerUtils
import org.lwjgl.input.Keyboard


object Etherwarper {
    var warping = false

    fun etherwarpToVec3(vec3: Vec3, silent: Boolean = false) {
        val rot = RotationUtils.getYawAndPitch(vec3)
        etherwarp(rot.first, rot.second, silent)
    }
    fun etherwarp(yaw: Float, pitch: Float, silent: Boolean = false){
        if (warping) return
        warping = true
        PlayerUtils.sneak()
        PlayerUtils.stopVelocity()
        val state = SwapManager.swapFromSBId("ASPECT_OF_THE_VOID")
        when (state) {
            SwapManager.SwapState.ALREADY_HELD -> {
                RotationUtils.rotate(yaw, pitch, silent, RotationUtils.Action.RightClick)
            }
            SwapManager.SwapState.SWAPPED -> {
                Scheduler.schedulePreTickTask {
                    RotationUtils.rotate(yaw, pitch, silent, RotationUtils.Action.RightClick)
                }
            }
            else -> return
        }
    }




    @SubscribeEvent
    fun onPacket(event: S08Event) { //u need the ct bypass cause zpew/zph
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