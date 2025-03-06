package com.github.wadey3636.noobroutes.utils

import com.github.wadey3636.noobroutes.features.AutoP3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import me.odinmain.OdinMain.mc
import me.odinmain.events.impl.PacketEvent
import me.odinmain.utils.multiply
import me.odinmain.utils.render.RenderUtils.renderX
import me.odinmain.utils.render.RenderUtils.renderY
import me.odinmain.utils.render.RenderUtils.renderZ
import me.odinmain.utils.skyblock.modMessage
import net.minecraft.client.settings.KeyBinding
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.gameevent.InputEvent
import org.lwjgl.input.Keyboard
import kotlin.math.pow

object AutoP3Utils {

    private val keyBindings = listOf(
        mc.gameSettings.keyBindForward,
        mc.gameSettings.keyBindLeft,
        mc.gameSettings.keyBindRight,
        mc.gameSettings.keyBindBack
    )

    private var hasUnpressed = false

    var awaitingTick = false

    fun unPressKeys() {
        Keyboard.enableRepeatEvents(false)
        keyBindings.forEach { KeyBinding.setKeyBindState(it.keyCode, false) }
        walking = false
    }

    private var walking = false
    private var motioning = false
    var direction = 0F

    fun startWalk(dir: Float) {
        walking = true
        direction  = dir
    }

    @SubscribeEvent
    fun awaitTick(event: PacketEvent) {
        if(!awaitingTick || event.packet !is C03PacketPlayer) return
        awaitingTick = false
        val speed = mc.thePlayer.capabilities.walkSpeed * 2.806
        mc.thePlayer.motionX = speed * Utils.xPart(direction)
        mc.thePlayer.motionZ = speed * Utils.zPart(direction)
    }

    @SubscribeEvent
    fun walk(event: TickEvent.ClientTickEvent) {
        if (!walking) return
        if (event.phase != TickEvent.Phase.START) return
        if(!mc.thePlayer.onGround) {
            walking = false
            motioning = true
        }
        keyBindings.forEach { KeyBinding.setKeyBindState(it.keyCode, false) }
        val speed = mc.thePlayer.capabilities.walkSpeed * 2.806
        mc.thePlayer.motionX = speed * Utils.xPart(direction)
        mc.thePlayer.motionZ = speed * Utils.zPart(direction)
    }

    @SubscribeEvent
    fun motion(event: TickEvent.ClientTickEvent) {
        if (!motioning) return
        if (event.phase != TickEvent.Phase.START) return
        modMessage("motioning")
        if(mc.thePlayer.onGround) {
            motioning = false
            walking = true
        }
        keyBindings.forEach { KeyBinding.setKeyBindState(it.keyCode, false) }
        val addedSpeed = mc.thePlayer.capabilities.walkSpeed * 0.0509
        mc.thePlayer.motionX += addedSpeed * Utils.xPart(direction)
        mc.thePlayer.motionZ += addedSpeed * Utils.zPart(direction)
    }

    fun distanceToRing(coords: Vec3): Double {
        if(AutoP3.frame) return (coords.xCoord-mc.thePlayer.renderX).pow(2)+(coords.zCoord-mc.thePlayer.renderZ).pow(2).pow(0.5)
        return (coords.xCoord-mc.thePlayer.posX).pow(2)+(coords.zCoord-mc.thePlayer.posZ).pow(2).pow(0.5)
    }

    fun ringCheckY(coords: Vec3): Boolean {
        if(AutoP3.frame) return coords.yCoord <= mc.thePlayer.renderY && coords.yCoord + 1 > mc.thePlayer.renderY
        return coords.yCoord <= mc.thePlayer.posY && coords.yCoord + 1 > mc.thePlayer.posY
    }

    @SubscribeEvent
    fun onKeyInput(event: InputEvent.KeyInputEvent) {
        if (!walking && !motioning) return
        val keyCode = Keyboard.getEventKey()
        if (keyCode != Keyboard.KEY_W && keyCode != Keyboard.KEY_A && keyCode != Keyboard.KEY_S && keyCode != Keyboard.KEY_D ) return
        val isPressed = Keyboard.getEventKeyState()
        if (!isPressed) {
            hasUnpressed = true
        }
        else if (hasUnpressed) {
            walking = false
            motioning = false
            hasUnpressed = false
        }
    }




}
