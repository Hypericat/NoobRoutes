package noobroutes.utils

import net.minecraft.client.settings.KeyBinding
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.Core.logger
import noobroutes.Core.mc
import noobroutes.events.BossEventDispatcher.inBoss
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.AutoP3.depth
import noobroutes.features.floor7.autop3.AutoP3.motionValue
import noobroutes.features.floor7.autop3.AutoP3.renderStyle
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.RingType
import noobroutes.features.floor7.autop3.rings.BlinkRing
import noobroutes.mixin.accessors.TimerFieldAccessor
import noobroutes.ui.clickgui.util.ColorUtil.withAlpha
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
import kotlin.math.log
import kotlin.math.pow
import kotlin.math.sin
import kotlin.reflect.KClass
import kotlin.text.replace

object AutoP3Utils {

    val keyBindings = listOf(
        mc.gameSettings.keyBindForward,
        mc.gameSettings.keyBindLeft,
        mc.gameSettings.keyBindRight,
        mc.gameSettings.keyBindBack
    )

    val tickSpeeds = mapOf(
        0 to 1.403,
        1 to 3.08,
        2 to 1.99,
        3 to 1.84,
        4 to 1.7,
        5 to 1.58,
        6 to 1.47,
        7 to 1.37,
        8 to 1.28,
        9 to 1.2,
        10 to 1.12,
        11 to 1.05,
        12 to 1.0,
        13 to 0.97,
    )

    private var xSpeed = 0.0
    private var zSpeed = 0.0

    var walkAfter = false
    var awaitingTick = false

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
        xSpeed = mc.thePlayer.motionX
        zSpeed = mc.thePlayer.motionZ
        walking = true
    }

    @SubscribeEvent
    fun onUnload(event: WorldEvent.Unload) {
        walking = false
        motioning = false
        testing = false
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onS08(event: PacketEvent.Receive) {
        if (event.packet !is S08PacketPlayerPosLook) return
        walking = false
        motioning = false
        testing = false
    }

    private var lastSpeed = 0.0

    var drag = 0.906339756
    var push = 0.03689255977

    @SubscribeEvent
    fun motion(event: ClientTickEvent) {
        if (!motioning || event.phase != TickEvent.Phase.START) return
        if (motionTicks == 0) setSpeed(1.4)
        if (motionTicks == 1) {
            if (mc.thePlayer.onGround) mc.thePlayer.jump()
            else {
                motioning = false
                modMessage("help im midair")
                return
            }
        }
        if (motionTicks == 1) setSpeed(AutoP3.tick1)
        else if (motionTicks == 2) {
            setSpeed(AutoP3.tick2)
            lastSpeed = AutoP3.tick2
        }
        else {
            lastSpeed *= drag
            lastSpeed += push
            setSpeed(lastSpeed)
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
        if (!AutoP3.noRotate || !inBoss) {
            lastLook = Pair(event.yaw, event.pitch)
        }
        else {
            event.yaw = lastLook.first
            event.pitch = lastLook.second
        }
    }

    @SubscribeEvent
    fun doTest(event: ClientTickEvent) {
        if (!testing || event.phase != TickEvent.Phase.START) return
        when (testTicks) {
            0 -> { setSpeed(AutoP3.tick0) }
            1 -> {
                if (mc.thePlayer.onGround) mc.thePlayer.jump()
                setSpeed(AutoP3.tick1)
            }
            2 -> { setSpeed(AutoP3.tick2) }
            3 -> { setSpeed(AutoP3.tick3) }
            4 -> { setSpeed(AutoP3.tick4) }
            5 -> { setSpeed(AutoP3.tick5) }
            6 -> { setSpeed(AutoP3.tick6) }
            7 -> { setSpeed(AutoP3.tick7) }
            8 -> { setSpeed(AutoP3.tick8) }
            9 -> { setSpeed(AutoP3.tick9) }
            10 -> { setSpeed(AutoP3.tick10) }
            11 -> { setSpeed(AutoP3.tick11) }
            12 -> { setSpeed(AutoP3.tick12) }
            13 -> { setSpeed(AutoP3.tick13) }
            14 -> { setSpeed(AutoP3.tick14) }
            15 -> { setSpeed(AutoP3.tick15) }
            else -> testing = false
        }
        testTicks++

    }

    private fun setSpeed(speed: Double) {
        mc.thePlayer.motionX = Utils.xPart(direction) * speed
        mc.thePlayer.motionZ = Utils.zPart(direction) * speed
    }

    @SubscribeEvent
    fun movement(event: ClientTickEvent) {
        if (mc.thePlayer == null || event.phase != TickEvent.Phase.START) return

        if (!walking) return

        if (mc.thePlayer.onGround)  {
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
        if (!walking && !motioning) return
        val keyCode = Keyboard.getEventKey()
        if (keyCode != Keyboard.KEY_W && keyCode != Keyboard.KEY_A && keyCode != Keyboard.KEY_S && keyCode != Keyboard.KEY_D ) return
        if (!Keyboard.getEventKeyState()) return
        walking = false
        motioning = false
    }

    fun renderRing(ring: Ring) {
        if (AutoP3.onlyCenter && ring !is BlinkRing && !ring.center) return
        when (renderStyle) {
            0 -> {
                Renderer.drawCylinder(ring.coords.add(Vec3(0.0, (0.45 * sin(System.currentTimeMillis().toDouble()/300)) + 0.528 , 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.GREEN, depth = depth)
                Renderer.drawCylinder(ring.coords.add(Vec3(0.0, (-0.45 * sin(System.currentTimeMillis().toDouble()/300)) + 0.528 , 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.GREEN, depth = depth)
                Renderer.drawCylinder(ring.coords.add(Vec3(0.0, 0.503, 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.GREEN, depth = depth)
                Renderer.drawCylinder(ring.coords.add(Vec3(0.0, 0.03, 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.DARK_GRAY, depth = depth)
                Renderer.drawCylinder(ring.coords.add(Vec3(0.0, 1.03, 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.DARK_GRAY, depth = depth)
            }
            1 -> Renderer.drawCylinder(ring.coords.add(Vec3(0.0, 0.03, 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.GREEN, depth = depth)
            2 -> RenderUtils.drawOutlinedAABB(ring.coords.subtract(0.5, 0.0, 0.5).toAABB(), Color.GREEN, thickness = 3, depth = depth)
        }
    }

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
        logger.info(classURL)
        val file = try {
            Paths.get(getBaseUrlForClassUrl(classURL).toURI())
        } catch (e: URISyntaxException) {
            logger.error("URI ERROR IN AUTO P3 UTILS: $e")
            return emptyMap()
        }
        logger.info("Base directory: $file")
        if (Files.isDirectory(file)) {
            walkDir(file, path)
        } else {
            walkJar(file, path)
        }
        logger.info("Found rings: ${foundRings.keys}")
        return foundRings
    }

    private fun walkDir(classRoot: Path, directory: String) {
        logger.info("Trying to find rings from directory")
        logger.info(classRoot.resolve(directory))
        val editedDir = directory.replace("/", ".")
        try {
            Files.walk(classRoot.resolve(directory)).use { classes ->
                classes.map<String?> { it: Path? -> classRoot.relativize(it).toString() }
                    .forEach { className: String? ->
                        logger.info("Found class: $className")
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
        logger.info("Found class: $norm, directory: $directory, ${norm.startsWith("$directory.")}")

        if (norm.startsWith("$directory.") && !norm.endsWith(".")) {
            logger.info("something happening")
            val clazz = Class.forName(norm)
            val annotation = clazz.getAnnotation(RingType::class.java)
            if (annotation == null) return
            logger.info("Found annotation: $annotation")
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
                    tryAddRingClass(next!!.getName(), editedDir)
                    zis.closeEntry()
                }
            }
        } catch (e: IOException) {
            throw java.lang.RuntimeException(e)
        }
    }

}
