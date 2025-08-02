package noobroutes.features.dungeon

import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.impl.ChatPacketEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.ServerTickEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.RotationUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.Vec2
import noobroutes.utils.Vec2i
import noobroutes.utils.isAir
import noobroutes.utils.routes.RouteUtils
import noobroutes.utils.routes.RouteUtils.setRotation
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.Dungeon
import noobroutes.utils.skyblock.dungeon.DungeonScan
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.Door
import noobroutes.utils.skyblock.dungeon.tiles.DoorType
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.toVec3
import org.lwjgl.input.Keyboard
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

object AutoBr: Module(
    name = "Auto Br",
    Keyboard.KEY_NONE,
    category = Category.DUNGEON,
    description = "Bloodrushes very fast (does it outside of map)"
) {
    private val autoStartBrToggle by BooleanSetting("Main Toggle", default = true, description = "u also need the module enabled for the snipe command")
    private val goOn1Delay by NumberSetting("go on delay", 13, 0, 20, description = "how long to wait before u actually go down (ticks)").withDependency { autoStartBrToggle }
    private val preLoadDelay by NumberSetting("Preload Delay", 0, 0, 20, description = "delay for preloading the map")
    private val silent by BooleanSetting("silent", description = "do silent rotations")
    private val noWait by BooleanSetting("Low Ping Pearls", default = false, description = "for ling ping players. makes pearls work")

    private val snipeTick by NumberSetting("Snipe Tick", 1, 0, 40, description = "how to align with the server ticks")

    private val BLOOD_MIDDLE_COORDS = BlockPos(0, 70, 0)

    private val MIDDLE_MAP_COORDS = Vec2i(-104, -104)

    private const val GO_STRAIGHT_ON_AOTV_PITCH = 4f

    private var waitingForClip = false

    var hasRunStarted = false

    private const val VERTICAL_TP_AMOUNT = 5

    private var serverTickCount = 0

    private var snipeCoords: Vec2i? = null

    private fun getFurthestDoor(): Vec2i {
        val bloodDoor = Dungeon.Info.dungeonList.find { it is Door && it.type == DoorType.BLOOD }
        if (bloodDoor != null) return Vec2i(bloodDoor.x, bloodDoor.z)

        val doors = Dungeon.Info.dungeonList.filter { it is Door && it.type == DoorType.WITHER }
        val furthest = doors.maxBy { getXZDistance(it.x.toDouble(), it.z.toDouble(), mc.thePlayer.posX, mc.thePlayer.posZ) }
        return Vec2i(furthest.x, furthest.z)
    }

    @SubscribeEvent
    fun onChat(event: ChatPacketEvent) {
        if (event.message == "Starting in 4 seconds." && autoStartBrToggle && !Dungeon.Info.uniqueRooms.any { it.name == "Blood" }) {
            Scheduler.scheduleServerTickTask(preLoadDelay) { testAutoPearl(true) }
        }
        else if (event.message == "Starting in 1 second.") {
            serverTickCount = 20
            Scheduler.scheduleServerTickTask(goOn1Delay) {
                hasRunStarted = true
                if (!Dungeon.Info.uniqueRooms.any { it.name == "Blood" }) return@scheduleServerTickTask
                if (autoStartBrToggle) testAutoPearl(true)
            }
        }
    }

    fun testAutoPearl(autoCommand: Boolean = false) {

        if (autoCommand && Dungeon.currentRoom?.name != "Entrance") return;

        Scheduler.schedulePreTickTask {
            setRotation(0f, 90f, silent)
            val clipDistance = mc.thePlayer.posY.toInt() - 62

            RouteUtils.pearlClip(clipDistance, silent)
            waitingForClip = true
        }

        Dungeon.Info.uniqueRooms.forEach { it.tiles }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true) //detect after pearlclip
    fun onS08(event: PacketEvent.Receive) {
        if (event.packet !is S08PacketPlayerPosLook || !waitingForClip) return
        waitingForClip = false

        Scheduler.schedulePreTickTask(1) {
            val state = if (LocationUtils.isSinglePlayer) SwapManager.swapFromId(277) else SwapManager.swapFromSBId("ASPECT_OF_THE_VOID")
            if (state == SwapManager.SwapState.UNKNOWN || state == SwapManager.SwapState.TOO_FAST) return@schedulePreTickTask

            Scheduler.schedulePostTickTask { Scheduler.scheduleFrameTask { repeat(VERTICAL_TP_AMOUNT) { PlayerUtils.airClick() } } }

            Scheduler.scheduleLowS08Task(VERTICAL_TP_AMOUNT - 1) {
                tpOver(snipeCoords)
                snipeCoords = null
            }
        }
    }

    private fun tpOver(otherCoords: Vec2i?) {
        Scheduler.schedulePreTickTask {
            val bloodRoom = Dungeon.Info.uniqueRooms.find { it.name == "Blood" }

            val coords = when {
                otherCoords != null -> otherCoords
                !hasRunStarted -> MIDDLE_MAP_COORDS
                bloodRoom != null -> bloodRoom.getRealCoords(BLOOD_MIDDLE_COORDS).toVec2i()
                else -> return@schedulePreTickTask modMessage("smth went wrong , probably couldnt find blood")
            }

            val yaw = RotationUtils.getYaw(coords)
            val distance = sqrt((mc.thePlayer.posX - coords.x).pow(2) + (mc.thePlayer.posZ - coords.z).pow(2))
            val aotvNumber = round(distance * 0.08333333333333333).toInt() // 1/12

            setRotation(yaw, GO_STRAIGHT_ON_AOTV_PITCH, silent)

            Scheduler.schedulePostTickTask { Scheduler.scheduleFrameTask { repeat(aotvNumber) { PlayerUtils.airClick() } } }

            if (coords == MIDDLE_MAP_COORDS) return@schedulePreTickTask

            Scheduler.scheduleLowS08Task(aotvNumber - 1) {
                tpUp(otherCoords != null)
            }
        }
    }

    private fun tpUp(isSnipe: Boolean) {
        Scheduler.schedulePreTickTask {

            setRotation(0f, -90f, silent)

            Scheduler.schedulePreTickTask {

                Scheduler.schedulePostTickTask { Scheduler.scheduleFrameTask { repeat(VERTICAL_TP_AMOUNT + if (isSnipe) 2 else 1) { PlayerUtils.airClick() } } }

                Scheduler.schedulePreTickTask {
                    SwapManager.swapFromName("pearl")

                    setRotation(0f, -90f, silent)

                    Scheduler.schedulePostTickTask { Scheduler.scheduleFrameTask { PlayerUtils.airClick() } }

                    throwOtherPearls(isSnipe)
                }
            }
        }
    }

    private fun throwOtherPearls(isSnipe: Boolean) {
        if (isSnipe) {
            Scheduler.schedulePreTickTask(1) { PlayerUtils.airClick() }
            Scheduler.schedulePreTickTask(3) { PlayerUtils.airClick() }
            return
        }

        if (noWait) {
            Scheduler.schedulePreTickTask(1) { PlayerUtils.airClick() }
            return
        }

        Scheduler.scheduleLowS08Task { PlayerUtils.airClick() }
    }

    fun snipeCommand(args: Array<out String>) {
        if (!canPearlClipToY62()) return modMessage("room goes to low")
        if (args.size < 2) {
            modMessage("gib room name")
            return
        }
        val name = args.drop(1).joinToString(" ").replace("_", " ")

        if (name.equals("boss", ignoreCase = true)) {
            snipeCoords = Vec2i(0, 0)
            modMessage("sniping boss")
            return
        }

        val center = Dungeon.Info.uniqueRooms.find { name.equals(it.name, true) }?.center ?: return modMessage("no room found")
        val worldX = DungeonScan.startX + center.x * (DungeonScan.roomSize shr 1)
        val worldZ = DungeonScan.startZ + center.z * (DungeonScan.roomSize shr 1)
        snipeCoords = Vec2i(worldX, worldZ)
        modMessage("sniping $name")
    }

    private fun reset() {
        waitingForClip = false
        hasRunStarted = false
        serverTickCount = 0
    }

    @SubscribeEvent
    fun onUnload(event: WorldEvent.Unload) {
        reset()
    }

    @SubscribeEvent
    fun onLoad(event: WorldEvent.Load) {
        reset()
    }

    private fun getXZDistance(x1: Double, z1: Double, x2: Double, z2: Double): Double {
        return sqrt((x2 - x1).pow(2) + (z2 - z1).pow(2))
    }

    private fun BlockPos.toVec2i(): Vec2i {
        return Vec2i(this.x, this.z)
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent) {
        serverTickCount++
        if (serverTickCount == snipeTick && snipeCoords != null) testAutoPearl()
        if (serverTickCount >= 40) serverTickCount = 0
    }

    private fun canPearlClipToY62(): Boolean {
        for (y in 0..64) {
            val pos = BlockPos(mc.thePlayer.posX.toInt(), y, mc.thePlayer.posZ.toInt())
            if (!isAir(pos)) {
                return false
            }
        }
        return true
    }
}