package noobroutes.features.floor7.autop3

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S18PacketEntityTeleport
import net.minecraft.util.MathHelper
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import noobroutes.Core.logger
import noobroutes.config.DataManager
import noobroutes.events.BossEventDispatcher
import noobroutes.events.BossEventDispatcher.inF7Boss
import noobroutes.events.impl.MoveEntityWithHeadingEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.Phase
import noobroutes.events.impl.TermOpenEvent
import noobroutes.events.impl.TerminalPhase
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.floor7.autop3.rings.BlinkRing
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.features.settings.impl.StringSetting
import noobroutes.utils.Scheduler
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.render.Color
import noobroutes.utils.render.RenderUtils
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import kotlin.collections.forEach
import kotlin.collections.iterator
import kotlin.jvm.java

object AutoP3New: Module (
    name = "AutoP3 New",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "less schizo AutoP3"
) {
    private var rings = mutableMapOf<String, MutableList<Ring>>()
    private val route by StringSetting("Route", "", description = "Route to use")
    private val renderIndex by BooleanSetting("Render Index", false, description = "Renders the index of the ring. Useful for creating routes")
    private val ringColor by ColorSetting("Ring Color", Color.GREEN, false)
    private var editMode by BooleanSetting("Edit Mode", false, description = "Disables ring actions")
    private val editModeKey by KeybindSetting("Toggle Edit Mode", Keyboard.KEY_NONE, "Toggles editmode on press").onPress {
        editMode = !editMode
        modMessage("edit Mode: " + !editMode)
    }


    var waitingRing: Ring? = null

    var leapedIds = mutableSetOf<Int>() //hyper pls forgive me but duplicates would murder me



    @SubscribeEvent
    fun renderRings(event: RenderWorldLastEvent) {
        if (!inF7Boss) return

        rings[route]?.forEachIndexed { i, ring ->
            ring.renderRing()
            if (renderIndex) Renderer.drawStringInWorld(i.toString(), ring.coords.add(Vec3(0.0, 0.6, 0.0)), Color.GREEN, depth = true, shadow = false)

            if (ring !is BlinkRing) return@forEachIndexed

            val lastPacket = ring.packets.last()

            ring.drawCylinderWithRingArgs(Vec3(lastPacket.positionX, lastPacket.positionY, lastPacket.positionZ), ringColor)

            RenderUtils.drawGradient3DLine(ring.packets.map { Vec3(it.positionX, it.positionY + 0.03, it.positionZ) }, Color.GREEN, Color.RED, 1F, true)
        }
    }

    @SubscribeEvent
    fun onMoveEntityWithHeading(event: MoveEntityWithHeadingEvent.Post) {
        if (!inF7Boss) return

        rings[route]?.forEach { ring ->
            if(!inF7Boss || mc.thePlayer.isSneaking || editMode ) return

            if (ring.inRing()) {
                if (ring.triggered) return@forEach
                ring.run()
            }

        }
    }

    @SubscribeEvent
    fun awaitingLeap(event: PacketEvent.Receive) {
        if (waitingRing?.leap != true || event.packet !is S18PacketEntityTeleport) return
        val ring = waitingRing ?: return

        val entity  = mc.theWorld.getEntityByID(event.packet.entityId)
        if (entity !is EntityPlayer) return

        val x = event.packet.x shr 5
        val y = event.packet.y shr 5
        val z = event.packet.z shr 5

        if (mc.thePlayer.getDistanceSq(x.toDouble(), y.toDouble(), z.toDouble()) < 5) leapedIds.add(event.packet.entityId)
        if (leapedIds.size == leapPlayers()) {

            if (!ring.inRing()) {
                waitingRing = null
                return
            }
            modMessage("everyone leaped")

            Scheduler.schedulePostMoveEntityWithHeadingTask {
                ring.doRing()
                waitingRing = null
            }
        }
    }

    @SubscribeEvent
    fun awaitingTerm(event: TermOpenEvent) {
        waitingRing?.let { ring ->
            if (!ring.term) return

            if (ring.inRing()) {
                Scheduler.schedulePostMoveEntityWithHeadingTask {
                    ring.doRing()
                    waitingRing = null
                }
            }
            else waitingRing = null
        }
    }

    @SubscribeEvent
    fun awaitingLeft(event: InputEvent.MouseInputEvent) {
        if (Mouse.getEventButton() != 0 || !Mouse.getEventButtonState()) return

        waitingRing?.let { ring ->
            if (ring.inRing()) {
                Scheduler.schedulePostMoveEntityWithHeadingTask {
                    ring.doRing()
                    waitingRing = null
                }
            }
            else waitingRing = null
        }
    }

    private fun leapPlayers(): Int {
        return when {
            BossEventDispatcher.currentBossPhase == Phase.P2 -> 1 //core
            BossEventDispatcher.currentTerminalPhase == TerminalPhase.S3 -> 3 //ee3
            else -> 4
        }
    }



    fun loadRings() {
        rings.clear()
        try {
            val file = DataManager.loadDataFromFileObject("rings")
            for (route in file) {
                val ringsInJson = mutableListOf<Ring>()
                route.value.forEach {
                    val ring = it.asJsonObject
                    val ringType = ring.get("type")?.asString ?: "Unknown"
                    val ringClass = RingType.getTypeFromName(ringType)
                    val instance: Ring = ringClass?.clazz?.getDeclaredConstructor()?.newInstance() ?: return@forEach
                    instance.coords = ring.get("coords").asVec3
                    instance.yaw = MathHelper.wrapAngleTo180_float(ring.get("yaw")?.asFloat ?: 0f)
                    instance.term = ring.get("term")?.asBoolean == true
                    instance.leap = ring.get("leap")?.asBoolean == true
                    instance.center = ring.get("center")?.asBoolean == true
                    instance.rotate = ring.get("rotate")?.asBoolean == true
                    instance.left = ring.get("left")?.asBoolean == true
                    instance.diameter = ring.get("diameter")?.asFloat ?: 1f
                    instance.height = ring.get("height")?.asFloat ?: 1f
                    instance.loadRingData(ring)
                    ringsInJson.add(instance)
                }
                rings[route.key] = ringsInJson
            }
        } catch (e: Exception) {
            modMessage("Error Loading Rings, Please Send Log to Wadey")
            logger.info(e)
        }
    }
    fun saveRings() {
        try {
            val outObj = JsonObject()
            for ((routeName, rings) in rings) {
                val ringArray = JsonArray().apply {
                    for (ring in rings) {
                        if (ring.type.canSave) add(ring.getAsJsonObject())
                    }
                }
                outObj.add(routeName, ringArray)
            }
            DataManager.saveDataToFile("rings", outObj)
        } catch (e: Exception) {
            modMessage("error saving")
            logger.error("error saving rings", e)
        }
    }
}