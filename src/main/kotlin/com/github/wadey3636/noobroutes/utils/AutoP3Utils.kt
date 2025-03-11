package com.github.wadey3636.noobroutes.utils

import com.github.wadey3636.noobroutes.features.AutoP3
import com.github.wadey3636.noobroutes.features.AutoP3.depth
import com.github.wadey3636.noobroutes.features.Ring
import com.github.wadey3636.noobroutes.features.RingTypes
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import me.odinmain.OdinMain.mc
import me.odinmain.events.impl.PacketEvent
import me.odinmain.utils.render.Color
import me.odinmain.utils.render.RenderUtils.renderX
import me.odinmain.utils.render.RenderUtils.renderY
import me.odinmain.utils.render.RenderUtils.renderZ
import me.odinmain.utils.render.Renderer
import me.odinmain.utils.skyblock.modMessage
import net.minecraft.client.settings.KeyBinding
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.lwjgl.input.Keyboard
import kotlin.math.pow
import kotlin.math.sin

object AutoP3Utils {

    private val keyBindings = listOf(
        mc.gameSettings.keyBindForward,
        mc.gameSettings.keyBindLeft,
        mc.gameSettings.keyBindRight,
        mc.gameSettings.keyBindBack
    )

    private var lastMotion = 0.0

    private var hasUnpressed = false
    var walkAfter = false
    var awaitingTick = false

    fun unPressKeys() {
        Keyboard.enableRepeatEvents(false)
        keyBindings.forEach { KeyBinding.setKeyBindState(it.keyCode, false) }
        walking = false
        motioning = false
    }

    var walking = false
    private var motioning = false
    var direction = 0F
    var yeeting = false
    var yeetTicks = 0

    fun startWalk(dir: Float) {
        if(mc.thePlayer.onGround) walking = true
        else motioning  = true
        hasUnpressed = false
        direction  = dir
    }

    @SubscribeEvent
    fun awaitTick(event: PacketEvent) {
        if(!awaitingTick || event.packet !is C03PacketPlayer) return
        awaitingTick = false
        val speed = mc.thePlayer.capabilities.walkSpeed * 2.806
        mc.thePlayer.motionX = speed * Utils.xPart(direction)
        mc.thePlayer.motionZ = speed * Utils.zPart(direction)
        if (!walkAfter) return
        if(mc.thePlayer.onGround) walking = true
        else if (!mc.thePlayer.onGround) motioning  = true
        walkAfter = false
    }

    @SubscribeEvent
    fun yeet(event: ClientTickEvent) {
        if (!yeeting || event.phase != TickEvent.Phase.START) return
        if (yeetTicks == 1) {
            if (mc.thePlayer.onGround) mc.thePlayer.jump()
            val speed = mc.thePlayer.capabilities.walkSpeed * 6.0
            mc.thePlayer.motionX = speed * Utils.xPart(direction)
            mc.thePlayer.motionZ = speed * Utils.zPart(direction)
        }
        if (yeetTicks == 2) {
            mc.thePlayer.motionX *= 0.7
            mc.thePlayer.motionZ *= 0.7
        }
        if (yeetTicks >= 2) {
            yeeting = false
            if (mc.thePlayer.onGround) {
                walking = true
                val speed = mc.thePlayer.capabilities.walkSpeed * 2.806
                mc.thePlayer.motionX = speed * Utils.xPart(direction)
                mc.thePlayer.motionZ = speed * Utils.zPart(direction)
                return
            }
            else {
                motioning = true
                mc.thePlayer.motionX *= 0.91
                mc.thePlayer.motionZ *= 0.91
                lastMotion = (mc.thePlayer.motionX.pow(2) + mc.thePlayer.motionZ.pow(2)).pow(0.5)
            }


        }
        yeetTicks++

    }

    @SubscribeEvent
    fun walk(event: TickEvent.ClientTickEvent) {
        if (!walking || event.phase != TickEvent.Phase.START) return
        if (!mc.thePlayer.onGround) {
            walking = false
            motioning = true
        }
        keyBindings.forEach { KeyBinding.setKeyBindState(it.keyCode, false) }
        val speed = mc.thePlayer.capabilities.walkSpeed * 2.806
        mc.thePlayer.motionX = speed * Utils.xPart(direction)
        mc.thePlayer.motionZ = speed * Utils.zPart(direction)
        lastMotion = speed
    }

    @SubscribeEvent
    fun motion(event: TickEvent.ClientTickEvent) {
        if (!motioning) return
        if (event.phase != TickEvent.Phase.START) return
        modMessage("motioning")
        keyBindings.forEach { KeyBinding.setKeyBindState(it.keyCode, false) }
        if(mc.thePlayer.onGround) {
            motioning = false
            walking = true
            val speed = mc.thePlayer.capabilities.walkSpeed * 2.806
            mc.thePlayer.motionX = speed * Utils.xPart(direction)
            mc.thePlayer.motionZ = speed * Utils.zPart(direction)
            return
        }
        val speed = lastMotion * 0.91 + mc.thePlayer.capabilities.walkSpeed * 0.05096001172887317
        mc.thePlayer.motionX = speed * Utils.xPart(direction)
        mc.thePlayer.motionZ = speed * Utils.zPart(direction)
        lastMotion = speed
    }

    fun distanceToRing(coords: Vec3): Double {
        if(AutoP3.frame) return (coords.xCoord-mc.thePlayer.renderX).pow(2)+(coords.zCoord-mc.thePlayer.renderZ).pow(2).pow(0.5)
        return (coords.xCoord-mc.thePlayer.posX).pow(2)+(coords.zCoord-mc.thePlayer.posZ).pow(2).pow(0.5)
    }

    fun ringCheckY(ring: Ring): Boolean {
        if(AutoP3.frame) return (ring.coords.yCoord <= mc.thePlayer.renderY && ring.coords.yCoord + 1 > mc.thePlayer.renderY && ring.type != RingTypes.BLINK) || (ring.coords.yCoord == mc.thePlayer.renderY)
        return (ring.coords.yCoord <= mc.thePlayer.posY && ring.coords.yCoord + 1 > mc.thePlayer.posY && ring.type != RingTypes.BLINK) || (ring.coords.yCoord == mc.thePlayer.posY)
    }

    @SubscribeEvent
    fun onKeyInput(event: InputEvent.KeyInputEvent) {
        if (!walking && !motioning && !yeeting) return
        val keyCode = Keyboard.getEventKey()
        if (keyCode != Keyboard.KEY_W && keyCode != Keyboard.KEY_A && keyCode != Keyboard.KEY_S && keyCode != Keyboard.KEY_D ) return
        val isPressed = Keyboard.getEventKeyState()
        if (!isPressed) {
            hasUnpressed = true
        }
        else if (hasUnpressed) {
            walking = false
            motioning = false
            yeeting = false
            hasUnpressed = false
        }
    }

    fun renderRing(ring: Ring) {
        //kotlin is disrespecting my carefully setup order of operations
        Renderer.drawCylinder(ring.coords.add(Vec3(0.0, (0.45 * sin(System.currentTimeMillis().toDouble()/300)) + 0.528 , 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.GREEN, depth = AutoP3.depth)
        Renderer.drawCylinder(ring.coords.add(Vec3(0.0, (-0.45 * sin(System.currentTimeMillis().toDouble()/300)) + 0.528 , 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.GREEN, depth = AutoP3.depth)
        Renderer.drawCylinder(ring.coords.add(Vec3(0.0, 0.503, 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.GREEN, depth = depth)
        Renderer.drawCylinder(ring.coords.add(Vec3(0.0, 0.03, 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.DARK_GRAY, depth = depth)
        Renderer.drawCylinder(ring.coords.add(Vec3(0.0, 1.03, 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.DARK_GRAY, depth = depth)
    }




}
