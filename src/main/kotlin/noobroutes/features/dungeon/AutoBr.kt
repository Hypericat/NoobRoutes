package noobroutes.features.dungeon

import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraft.util.Vec3i
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.events.impl.BlockChangeEvent
import noobroutes.events.impl.BloodRoomSpawnEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.RoomEnterEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.misc.EWPathfinderModule
import noobroutes.features.routes.nodes.autoroutes.PearlClip
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.RotationUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.Utils.isStart
import noobroutes.utils.isAir
import noobroutes.utils.routes.RouteUtils
import noobroutes.utils.routes.RouteUtils.setRotation
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.Dungeon
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.tiles.Rotations
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.toBlockPos
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
    private val testPearls by KeybindSetting("test pearls", Keyboard.KEY_NONE, "tests").onPress { testAutoPearl() }

    private val BLOOD_MIDDLE_COORDS = BlockPos(0, 99, 0)

    private val MIDDLE = Vec3i(-104, 0, -104)

    private const val GO_STRAIGHT_ON_AOTV_PITCH = 4f

    private var waitingForClip = false
    private var waitingForS08 = false

    private val MIDDLE_DOOR_BLOCK = BlockPos(0, 69, 16)

    private var hasRunStarted = false

    private var userPressed = false

    private const val VERTICAL_TP_AMOUNT = 5

    @SubscribeEvent
    fun onBlockChange(event: BlockChangeEvent) {
        val room = Dungeon.currentRoom ?: return
        if (room.name != "Entrance") return

        if (event.pos == room.getRealCoords(MIDDLE_DOOR_BLOCK) && event.update.block == Blocks.barrier && !hasRunStarted) {
            hasRunStarted = true
            //if (userPressed) { Scheduler.schedulePreTickTask(10) { testAutoPearl() } }
            if (userPressed) testAutoPearl()
        }
    }

    fun testAutoPearl() {
        if (mc.thePlayer.posY != 72.0 && mc.thePlayer.posY != 71.0) return modMessage("need to be at y 71/72")
        Scheduler.schedulePreTickTask {
            val pearlCLipNode = PearlClip(mc.thePlayer.positionVector, if (mc.thePlayer.posY == 72.0) 10 else 9)
            pearlCLipNode.run()
            waitingForClip = true
            userPressed = true
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true) //detect after pearlclip
    fun onS08(event: PacketEvent.Receive) {
        if (event.packet !is S08PacketPlayerPosLook || !waitingForClip) return
        waitingForClip = false

        Scheduler.schedulePreTickTask {
            val state = if (LocationUtils.isSinglePlayer) SwapManager.swapFromId(277) else SwapManager.swapFromSBId("ASPECT_OF_THE_VOID")
            if (state == SwapManager.SwapState.UNKNOWN || state == SwapManager.SwapState.TOO_FAST) return@schedulePreTickTask
            setRotation(0f, 90f, false)

            Scheduler.schedulePreTickTask { repeat(VERTICAL_TP_AMOUNT) { PlayerUtils.airClick() } }

            Scheduler.scheduleLowS08Task(VERTICAL_TP_AMOUNT - 1) {
                Scheduler.schedulePreTickTask {
                    val bloodRoom = Dungeon.Info.uniqueRooms.find { it.name == "Blood" }

                    val coords = if (!hasRunStarted || bloodRoom == null) MIDDLE else bloodRoom.getRealCoords(BLOOD_MIDDLE_COORDS)

                    val yaw = RotationUtils.getYaw(coords.x.toDouble(), coords.z.toDouble())
                    val distance = sqrt((mc.thePlayer.posX - coords.x).pow(2) + (mc.thePlayer.posZ - coords.z).pow(2))
                    val aotvNumber = round(distance * 0.08333333333333333333333333333333).toInt() // 1/12

                    setRotation(yaw, GO_STRAIGHT_ON_AOTV_PITCH, false)

                    Scheduler.schedulePreTickTask { repeat(aotvNumber) { PlayerUtils.airClick() } }
                }
            }
        }
    }

    @SubscribeEvent
    fun onBloodEnter(event: RoomEnterEvent) {
        if (event.room?.name != "Blood" || mc.thePlayer.posY > 40) return
        Scheduler.schedulePreTickTask {
            setRotation(null, -90f, false)
            Scheduler.schedulePreTickTask { repeat(VERTICAL_TP_AMOUNT) { PlayerUtils.airClick() } }
            waitingForS08 = true
        }
    }

    @SubscribeEvent
    fun onDifferentS08(event: PacketEvent.Receive) {
        if (event.packet !is S08PacketPlayerPosLook || !waitingForS08 || event.packet.y != 64.0) return
        waitingForS08 = false

        Scheduler.schedulePreTickTask {
            SwapManager.swapFromName("pearl")

            Scheduler.schedulePreTickTask { PlayerUtils.airClick() }

            Scheduler.scheduleLowS08Task {
                PlayerUtils.airClick()
            }
        }
    }

    @SubscribeEvent
    fun onUnload(event: WorldEvent.Unload) {
        waitingForS08 = false
        waitingForClip = false
        hasRunStarted = false
        userPressed = false
    }
}