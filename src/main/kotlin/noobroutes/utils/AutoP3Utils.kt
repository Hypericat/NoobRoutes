package noobroutes.utils

import net.minecraft.client.settings.KeyBinding
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.Core.logger
import noobroutes.Core.mc
import noobroutes.events.BossEventDispatcher
import noobroutes.events.BossEventDispatcher.inF7Boss
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.Phase
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.AutoP3.depth
import noobroutes.features.floor7.autop3.AutoP3.renderStyle
import noobroutes.features.floor7.autop3.AutoP3.walkFix
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType
import noobroutes.features.floor7.autop3.rings.BlinkRing
import noobroutes.mixin.accessors.TimerFieldAccessor
import noobroutes.utils.render.Color
import noobroutes.utils.render.RenderUtils
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
import java.io.IOException
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.math.pow
import kotlin.math.sin
import kotlin.reflect.KClass

@Suppress("Unused")
object AutoP3Utils {

    val keyBindings = listOf(
        mc.gameSettings.keyBindForward,
        mc.gameSettings.keyBindLeft,
        mc.gameSettings.keyBindRight,
        mc.gameSettings.keyBindBack
    )

    fun unPressKeys(stop: Boolean = true) {
        Keyboard.enableRepeatEvents(false)
        keyBindings.forEach { KeyBinding.setKeyBindState(it.keyCode, false) }
        if (!stop) return
        walking = false
        motioning = false
        testing = false
    }

    fun rePressKeys() {
        keyBindings.forEach { KeyBinding.setKeyBindState(it.keyCode, Keyboard.isKeyDown(it.keyCode)) }
    }

    var walking = false
    var direction = 0F
    var motioning = false
    var motionTicks = 0
    var testing = false
    var testTicks = 0

    fun startWalk(dir: Float) {
        direction = dir
        walking = true
    }

    @SubscribeEvent
    fun onUnload(event: WorldEvent.Unload) {
        stopShit()
    }

    fun stopShit() {
        walking = false
        motioning = false
        testing = false
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onS08(event: PacketEvent.Receive) {
        if (event.packet is S08PacketPlayerPosLook) stopShit()
    }

    private var lastSpeed = 0.0
    var scale = 1f

    private const val DRAG = 0.9063338661881611
    private const val PUSH = 0.036901383361851
    private const val TICK1 = 3.08
    private const val TICK2 = 1.99

    @SubscribeEvent
    fun motion(event: ClientTickEvent) {
        if (!motioning || event.phase != TickEvent.Phase.START) return
        when (motionTicks) {
            0 -> setSpeed(1.4)
            1 -> {
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.jump()
                    setSpeed(TICK1 * scale)
                }
                else {
                    motioning = false
                    modMessage("help im midair")
                    return
                }
            }
            2 -> {
                setSpeed(TICK2 * scale)
                lastSpeed = TICK2
            }
            else -> {
                lastSpeed *= DRAG
                lastSpeed += PUSH
                setSpeed(lastSpeed * scale)
            }
        }
        if (motionTicks > 1 && mc.thePlayer.onGround) {
            startWalk(direction)
            motioning = false
        }
        motionTicks++
    }

    private var lastLook = Pair(0f,0f)

    @SubscribeEvent
    fun noTurn(event: MotionUpdateEvent.Pre) {
        if (!AutoP3.noRotate || BossEventDispatcher.currentBossPhase != Phase.P3) {
            lastLook = Pair(event.yaw, event.pitch)
        }
        else {
            event.yaw = lastLook.first
            event.pitch = lastLook.second
        }
    }

    private fun setSpeed(speed: Double) {
        mc.thePlayer.motionX = Utils.xPart(direction) * speed
        mc.thePlayer.motionZ = Utils.zPart(direction) * speed
    }

    var airTicks = 0
    var jumping = false

    private const val JUMP_SPEED = 6.0075
    private const val SPRINT_MULTIPLIER = 1.3

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun movement(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null) return

        if (mc.thePlayer.onGround) {
            airTicks = 0
        } else {
            ++airTicks
        }

        if (mc.thePlayer.isInWater || mc.thePlayer.isInLava || !walking) return
        val speed = mc.thePlayer.aiMoveSpeed.toDouble()

        if (airTicks < 1) {
            var speedMultiplier = 2.806
            if (jumping) {
                jumping = false
                speedMultiplier = JUMP_SPEED
            }
            mc.thePlayer.motionX = Utils.xPart(direction) * speed * speedMultiplier
            mc.thePlayer.motionZ = Utils.zPart(direction) * speed * speedMultiplier
            return
        }

        val movementFactor = if (mc.thePlayer.onGround || (airTicks == 1 && mc.thePlayer.motionY < 0 && AutoP3.walkFix != 0)) {
            speed * if (walkFix == 2) SPRINT_MULTIPLIER else 1.0
        } else {
            0.02 * SPRINT_MULTIPLIER
        }
        mc.thePlayer.motionX += movementFactor * Utils.xPart(direction)
        mc.thePlayer.motionZ += movementFactor * Utils.zPart(direction)
    }

    fun distanceToRingSq(coords: Vec3): Double {
        return (coords.xCoord-mc.thePlayer.posX).pow(2)+(coords.zCoord-mc.thePlayer.posZ).pow(2)
    }

    fun ringCheckY(ring: Ring): Boolean {
        return (ring.coords.yCoord <= mc.thePlayer.posY && ring.coords.yCoord + 1 > mc.thePlayer.posY && ring !is BlinkRing && !ring.center) || (ring.coords.yCoord == mc.thePlayer.posY)
    }

    @SubscribeEvent
    fun onKeyInput(event: InputEvent.KeyInputEvent) {
        if (!walking && !motioning) return
        val keyCode = Keyboard.getEventKey()
        if (keyCode != Keyboard.KEY_W && keyCode != Keyboard.KEY_A && keyCode != Keyboard.KEY_S && keyCode != Keyboard.KEY_D ) return
        if (!Keyboard.getEventKeyState()) return
        walking = false
        motioning = false
        testing = false
    }

    val ringColors = mapOf(
        "Jump" to Color(255, 0, 255),
        "Stop" to Color(255, 0, 0),
        "Boom" to Color(0, 255, 255),
        "HClip" to Color(0, 0, 0),
        "Walk" to Color(255, 0, 255),
        "LavaClip" to Color(255, 255, 0),
        "Blink" to Color(255, 255, 255),
        "Clamp" to Color(255, 0, 255),
        "Insta" to Color(0, 0, 0),
        "Motion" to Color(255, 0, 255),
        "Speed" to Color(255, 255, 255),
        "Test" to Color(255, 0, 255),
    )



    fun setGameSpeed(speed: Float) {
        val accessor = mc as TimerFieldAccessor

        devMessage(accessor.timer.timerSpeed)
        accessor.timer.timerSpeed = speed
        accessor.timer.updateTimer()
        devMessage("Set Timer: $speed, ${System.currentTimeMillis()}")
    }




    var foundRings = mutableMapOf<String, KClass<out Ring>>()

    fun discoverRings(packageName: String): Map<String, KClass<out Ring>> {
        foundRings = mutableMapOf()
        val path = packageName.replace('.', '/')
        val classURL = this.javaClass.protectionDomain.codeSource.location
        val file = try {
            Paths.get(getBaseUrlForClassUrl(classURL).toURI())
        } catch (e: URISyntaxException) {
            logger.error("URI ERROR IN AUTO P3 UTILS: $e")
            return emptyMap()
        }
        if (Files.isDirectory(file)) {
            walkDir(file, path)
        } else {
            walkJar(file, path)
        }
        logger.info("Found rings: ${foundRings.keys}")
        return foundRings
    }

    private fun walkDir(classRoot: Path, directory: String) {
        val editedDir = directory.replace("/", ".")
        try {
            Files.walk(classRoot.resolve(directory)).use { classes ->
                classes.map<String?> { it: Path? -> classRoot.relativize(it).toString() }
                    .forEach { className: String? ->
                        tryAddRingClass(className, editedDir)
                    }
            }
        } catch (e: IOException) {
            throw java.lang.RuntimeException(e)
        }
    }



    fun tryAddRingClass(className: String?, directory: String) {
        if (className == null) return
        val norm = (if (className.endsWith(".class")) className.substring(
            0,
            className.length - ".class".length
        ) else className)
            .replace("\\", "/")
            .replace("/", ".")
        if (norm.startsWith("$directory.") && !norm.endsWith(".")) {
            val clazz = Class.forName(norm)
            val annotation = clazz.getAnnotation(RingType::class.java)
            if (annotation == null) return
            @Suppress("UNCHECKED_CAST")
            foundRings[annotation.name] = clazz.kotlin as KClass<out Ring>

        }
    }


    fun getBaseUrlForClassUrl(classUrl: URL): URL {
        val string = classUrl.toString()
        if (classUrl.protocol == "jar") {
            try {
                return URL(string.substring(4).split("!".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
            } catch (e: MalformedURLException) {
                throw java.lang.RuntimeException(e)
            }
        }
        if (string.endsWith(".class")) {
            try {
                return URL(
                    string.replace("\\", "/")
                        .replace(
                            javaClass.getCanonicalName()
                                .replace(".", "/") + ".class", ""
                        ))
            } catch (e: MalformedURLException) {
                throw java.lang.RuntimeException(e)
            }
        }
        return classUrl
    }

    private fun walkJar(file: Path, directory: String) {
        println("Trying to find rings from jar file")
        val editedDir = directory.replace("/", ".")
        try {
            ZipInputStream(Files.newInputStream(file)).use { zis ->
                var next: ZipEntry?
                while ((zis.getNextEntry().also { next = it }) != null) {
                    val name = next!!.name
                    if (!name.startsWith(directory) || !name.endsWith(".class")) {
                        zis.closeEntry()
                        continue
                    }
                    tryAddRingClass(next.getName(), editedDir)
                    zis.closeEntry()
                }
            }
        } catch (e: IOException) {
            throw java.lang.RuntimeException(e)
        }
    }

}
