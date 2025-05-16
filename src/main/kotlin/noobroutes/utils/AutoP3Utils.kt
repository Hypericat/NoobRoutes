package noobroutes.utils

import net.minecraft.client.settings.KeyBinding
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.Core.logger
import noobroutes.Core.mc
import noobroutes.events.impl.PacketEvent
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.AutoP3.depth
import noobroutes.features.floor7.autop3.AutoP3.motionValue
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType
import noobroutes.features.floor7.autop3.rings.BlinkRing
import noobroutes.mixin.accessors.TimerFieldAccessor
import noobroutes.utils.render.Color
import noobroutes.utils.render.RenderUtils.renderX
import noobroutes.utils.render.RenderUtils.renderY
import noobroutes.utils.render.RenderUtils.renderZ
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.devMessage
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.jar.JarFile
import kotlin.math.pow
import kotlin.math.sin
import kotlin.reflect.KClass

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
                if (mc.thePlayer.onGround) mc.thePlayer.jump()
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

    fun distanceToRingSq(coords: Vec3): Double {
        return (coords.xCoord-mc.thePlayer.posX).pow(2)+(coords.zCoord-mc.thePlayer.posZ).pow(2)
    }

    fun ringCheckY(ring: Ring): Boolean {
        return (ring.coords.yCoord <= mc.thePlayer.posY && ring.coords.yCoord + 1 > mc.thePlayer.posY && ring !is BlinkRing) || (ring.coords.yCoord == mc.thePlayer.posY)
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

    fun renderRing(ring: Ring) {
        if (AutoP3.onlyCenter && ring !is BlinkRing && !ring.center) return
        if (AutoP3.simpleRings) {
            Renderer.drawCylinder(ring.coords.add(Vec3(0.0, 0.03, 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.GREEN, depth = depth)
            return
        }
        //kotlin is disrespecting my carefully setup order of operations
        Renderer.drawCylinder(ring.coords.add(Vec3(0.0, (0.45 * sin(System.currentTimeMillis().toDouble()/300)) + 0.528 , 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.GREEN, depth = depth)
        Renderer.drawCylinder(ring.coords.add(Vec3(0.0, (-0.45 * sin(System.currentTimeMillis().toDouble()/300)) + 0.528 , 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.GREEN, depth = depth)
        Renderer.drawCylinder(ring.coords.add(Vec3(0.0, 0.503, 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.GREEN, depth = depth)
        Renderer.drawCylinder(ring.coords.add(Vec3(0.0, 0.03, 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.DARK_GRAY, depth = depth)
        Renderer.drawCylinder(ring.coords.add(Vec3(0.0, 1.03, 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.DARK_GRAY, depth = depth)
    }

    fun setGameSpeed(speed: Float) {
        val accessor = mc as TimerFieldAccessor

        devMessage(accessor.timer.timerSpeed)
        accessor.timer.timerSpeed = speed
        accessor.timer.updateTimer()
        devMessage("Set Timer: $speed, ${System.currentTimeMillis()}")
    }

    fun discoverRings(packageName: String): Map<String, KClass<out Ring>> {
        val result = mutableMapOf<String, KClass<out Ring>>()
        val classLoader = Thread.currentThread().contextClassLoader
        val path = packageName.replace('.', '/')

        val classUri = classLoader.getResource(path)
            ?: throw RuntimeException("Package path not found: $path")

        val basePath = Paths.get(classUri.toURI())

        if (Files.isDirectory(basePath)) {
            // In dev environment
            Files.walk(basePath).forEach { filePath ->
                if (filePath.toString().endsWith(".class")) {
                    val relativePath = basePath.relativize(filePath).toString()
                    val className = "$packageName." + relativePath
                        .removeSuffix(".class")
                        .replace(File.separatorChar, '.')

                    try {
                        val clazz = Class.forName(className)
                        if (Ring::class.java.isAssignableFrom(clazz)) {
                            val annotation = clazz.getAnnotation(RingType::class.java)
                            if (annotation != null) {
                                @Suppress("UNCHECKED_CAST")
                                result[annotation.name] = clazz.kotlin as KClass<out Ring>
                            }
                        }
                    } catch (e: Throwable) {
                        println("Skipping $className due to error: ${e.message}")
                    }
                }
            }
        } else {
            //Non-Dev Environment
            val jarPath = File(classUri.toURI()).toString()
            val jarFile = JarFile(jarPath)

            jarFile.entries().iterator().forEachRemaining { entry ->
                if (!entry.name.endsWith(".class")) return@forEachRemaining
                if (!entry.name.startsWith(path)) return@forEachRemaining

                val className = entry.name
                    .removeSuffix(".class")
                    .replace('/', '.')

                try {
                    val clazz = Class.forName(className)
                    if (Ring::class.java.isAssignableFrom(clazz)) {
                        val annotation = clazz.getAnnotation(RingType::class.java)
                        if (annotation != null) {
                            @Suppress("UNCHECKED_CAST")
                            result[annotation.name] = clazz.kotlin as KClass<out Ring>
                        }
                    }
                } catch (e: Throwable) {
                   logger.error("Skipping $className due to error: ${e.message}")
                }
            }
        }

        return result
    }


}
