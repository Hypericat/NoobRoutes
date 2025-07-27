package noobroutes.features.floor7.autop3

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.server.S18PacketEntityTeleport
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import noobroutes.Core.logger
import noobroutes.config.DataManager
import noobroutes.events.BossEventDispatcher
import noobroutes.events.BossEventDispatcher.inF7Boss
import noobroutes.events.impl.*
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.floor7.autop3.rings.BlinkRing
import noobroutes.features.settings.impl.*
import noobroutes.utils.Scheduler
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.render.Color
import noobroutes.utils.render.RenderUtils
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

object AutoP3: Module (
    name = "AutoP3",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "schizo since version 0.0.1"
) {
    private var rings = mutableMapOf<String, MutableList<Ring>>()
    private val route by StringSetting("Route", "", description = "Route to use")
    private val renderIndex by BooleanSetting("Render Index", false, description = "Renders the index of the ring. Useful for creating routes")
    private val ringColor by ColorSetting("Ring Color", Color.GREEN, false, description = "color of the rings")
    private var editMode by BooleanSetting("Edit Mode", false, description = "Disables ring actions")
    private val editModeKey by KeybindSetting("Toggle Edit Mode", Keyboard.KEY_NONE, "Toggles editmode on press").onPress {
        editMode = !editMode
        modMessage("edit Mode: " + !editMode)
    }
    val walkBoost by SelectorSetting("Walk Boost", "none", arrayListOf("none", "normal", "big"), description = "how much of a boost to apply walking of edges")

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

    @SubscribeEvent(priority = EventPriority.HIGH)
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

    /*
    @SubscribeEvent
    fun renderMovement(event: RenderWorldLastEvent) {
        if(!BossEventDispatcher.inF7Boss || movementPackets.isEmpty() || lastMovementedC03 == null || !AutoP3.mode) return
        val firstPacket = movementPackets.first()
        val beforeFirst = lastMovementedC03 ?: return
        val xDiff = firstPacket.positionX - beforeFirst.positionX
        val yDiff = firstPacket.positionY - beforeFirst.positionY
        val zDiff = firstPacket.positionZ - beforeFirst.positionZ
        val timeAlong = event.partialTicks
        val xPos = beforeFirst.positionX + xDiff * timeAlong
        val yPos = beforeFirst.positionY + yDiff * timeAlong
        val zPos = beforeFirst.positionZ + zDiff * timeAlong
        Renderer.drawBox(
            AxisAlignedBB(
                xPos + 0.3,
                yPos,
                zPos + 0.3,
                xPos - 0.3,
                yPos + 1.8,
                zPos - 0.3
            ), Color.Companion.GREEN, fillAlpha = 0, outlineWidth = 1.5F)
    }

    @SubscribeEvent
    fun movement(event: PacketEvent.Send) {
        if (movementPackets.isEmpty() || event.packet !is C03PacketPlayer || AutoSS.dontCancel) return
        if (skip) {
            skip = false
            return
        }
        event.isCanceled = true
        skip = true
        PacketUtils.sendPacket(movementPackets[0])
        if (!AutoP3.mode) Core.mc.thePlayer.setPosition(movementPackets[0].positionX, movementPackets[0].positionY, movementPackets[0].positionZ)
        if (movementPackets.size == 1) {
            Core.mc.thePlayer.motionY = endY
            Core.mc.thePlayer.setPosition(movementPackets[0].positionX, movementPackets[0].positionY, movementPackets[0].positionZ)
            lastBlink = System.currentTimeMillis()
            AutoP3.isAligned = true
        }
        lastMovementedC03 = movementPackets.removeFirst()
    }*/

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