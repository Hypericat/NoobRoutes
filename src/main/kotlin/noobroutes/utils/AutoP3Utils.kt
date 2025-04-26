package noobroutes.utils

import noobroutes.Core.mc
import noobroutes.events.impl.PacketEvent
import noobroutes.features.floor7.AutoP3
import noobroutes.features.floor7.AutoP3.depth
import noobroutes.features.floor7.AutoP3.motionValue
import noobroutes.features.floor7.AutoP3.waitingLeap
import noobroutes.features.floor7.AutoP3.waitingTerm
import noobroutes.features.floor7.Ring
import noobroutes.features.floor7.RingTypes
import noobroutes.utils.render.Color
import noobroutes.utils.render.RenderUtils.renderX
import noobroutes.utils.render.RenderUtils.renderY
import noobroutes.utils.render.RenderUtils.renderZ
import noobroutes.utils.render.Renderer
import net.minecraft.client.settings.KeyBinding
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import kotlin.math.pow
import kotlin.math.sin

object AutoP3Utils {

    val keyBindings = listOf(
        mc.gameSettings.keyBindForward,
        mc.gameSettings.keyBindLeft,
        mc.gameSettings.keyBindRight,
        mc.gameSettings.keyBindBack
    )

    private var xSpeed = 0.0
    private var zSpeed = 0.0
    private var air = 0

    var walkAfter = false
    var awaitingTick = false

    fun unPressKeys(stop: Boolean = true) {
        Keyboard.enableRepeatEvents(false)
        keyBindings.forEach { KeyBinding.setKeyBindState(it.keyCode, false) }
        if (!stop) return
        walking = false
        yeeting = false
    }

    fun rePressKeys() {
        keyBindings.forEach { KeyBinding.setKeyBindState(it.keyCode, Keyboard.isKeyDown(it.keyCode)) }
    }

    var walking = false
    var direction = 0F
    var yeeting = false
    var yeetTicks = 0

    fun startWalk(dir: Float) {
        direction = dir
        xSpeed = mc.thePlayer.motionX
        zSpeed = mc.thePlayer.motionZ
        walking = true
    }

    @SubscribeEvent
    fun awaitTick(event: PacketEvent) {
        if(!awaitingTick || event.packet !is C03PacketPlayer) return
        awaitingTick = false
        val speed = mc.thePlayer.capabilities.walkSpeed * 2.806
        mc.thePlayer.motionX = speed * Utils.xPart(direction)
        mc.thePlayer.motionZ = speed * Utils.zPart(direction)
        xSpeed = speed * Utils.xPart(direction)
        zSpeed = speed * Utils.zPart(direction)
        if (walkAfter) {
            walkAfter = false
            Scheduler.schedulePreTickTask { walking = true }
        }
    }

    @SubscribeEvent
    fun onS08(event: PacketEvent.Receive) {
        if (event.packet !is S08PacketPlayerPosLook) return
        walking = false
        yeeting = false
    }

    @SubscribeEvent
    fun yeet(event: ClientTickEvent) {
        if (!yeeting || event.phase != TickEvent.Phase.START) return
        when (yeetTicks) {
            0 -> {
                val speed = mc.thePlayer.capabilities.walkSpeed * 0.1 / 0.91
                mc.thePlayer.motionX = -Utils.xPart(direction) * speed
                mc.thePlayer.motionZ = -Utils.zPart(direction) * speed
            }
            1 -> {
                mc.thePlayer.jump()
                val speed = mc.thePlayer.capabilities.walkSpeed * 5.5 / 0.91
                mc.thePlayer.motionX = Utils.xPart(direction) * speed
                mc.thePlayer.motionZ = Utils.zPart(direction) * speed
            }
            2 -> {
                mc.thePlayer.motionX *= 0.7 / 0.91
                mc.thePlayer.motionZ *= 0.7 / 0.91
            }
        }
        if (yeetTicks > 1) {
            if (mc.thePlayer.onGround) {
                val speed = mc.thePlayer.capabilities.walkSpeed * 2.806
                mc.thePlayer.motionX = Utils.xPart(direction) * speed
                mc.thePlayer.motionZ = Utils.zPart(direction) * speed
            }
            else {
                mc.thePlayer.motionX += mc.thePlayer.capabilities.walkSpeed * motionValue/10000 * Utils.xPart(direction)
                mc.thePlayer.motionZ += mc.thePlayer.capabilities.walkSpeed * motionValue/10000 * Utils.zPart(direction)
            }
        }
        yeetTicks++

    }

    @SubscribeEvent
    fun movement(event: ClientTickEvent) {
        if (mc.thePlayer == null || event.phase != TickEvent.Phase.START) return
        if (mc.thePlayer.onGround) air = 0
        else air++

        if (!walking) return

        if (air <= 1)  {
            val speed = mc.thePlayer.capabilities.walkSpeed * 2.806
            xSpeed = speed * Utils.xPart(direction)
            zSpeed = speed * Utils.zPart(direction)
            mc.thePlayer.motionX = speed * Utils.xPart(direction)
            mc.thePlayer.motionZ = speed * Utils.zPart(direction)
        }
        else {
            xSpeed = xSpeed * 0.91 + motionValue/10000 * mc.thePlayer.capabilities.walkSpeed * Utils.xPart(direction)
            zSpeed = zSpeed * 0.91 + motionValue/10000 * mc.thePlayer.capabilities.walkSpeed * Utils.zPart(direction)
            mc.thePlayer.motionX = xSpeed
            mc.thePlayer.motionZ = zSpeed
        }
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
        if (!walking && !yeeting) return
        val keyCode = Keyboard.getEventKey()
        if (keyCode != Keyboard.KEY_W && keyCode != Keyboard.KEY_A && keyCode != Keyboard.KEY_S && keyCode != Keyboard.KEY_D ) return
        if (!Keyboard.getEventKeyState()) return
        walking = false
        yeeting = false
    }

    @SubscribeEvent
    fun onLeftMouse(event: InputEvent.MouseInputEvent) {
        if (!waitingTerm && !waitingLeap) return
        val isLeft = Mouse.getEventButton() == 0
        if (isLeft && Mouse.getEventButtonState()) walking = true
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
