package noobroutes.features.dungeon

import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3i
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.impl.ChatPacketEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.RoomEnterEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.render.ClickGUIModule
import noobroutes.features.routes.nodes.autoroutes.PearlClip
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.ui.clickgui.ClickGUI
import noobroutes.utils.RotationUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.routes.RouteUtils
import noobroutes.utils.routes.RouteUtils.setRotation
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.Dungeon
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.modMessage
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
    private val goOn1 by BooleanSetting("go on 1", description = "goes down on 1")
    private val goOn1Delay by NumberSetting("go on delay", 2, 0, 20, description = "how long to wait before u actually go down (ticks)").withDependency { goOn1 }
    private val noWait by BooleanSetting("faster pearls", description = "pearls diffrently, might be faster")
    private val silent by BooleanSetting("silent", description = "do silent rotations")

    //private val meme by KeybindSetting("send 60 c08s", Keyboard.KEY_NONE, "for the meme").withDependency { ClickGUIModule.devMode }.onPress { repeat(60) { PlayerUtils.airClick() } }

    private val doTest by BooleanSetting("testing shit", description = "tests shit").withDependency { ClickGUIModule.devMode }

    private val BLOOD_MIDDLE_COORDS = BlockPos(0, 99, 0)

    private val MIDDLE = Vec3i(-104, 0, -104)

    private const val GO_STRAIGHT_ON_AOTV_PITCH = 4f

    private var waitingForClip = false

    private var hasRunStarted = false

    private const val VERTICAL_TP_AMOUNT = 5

    @SubscribeEvent
    fun onChat(event: ChatPacketEvent) {
        if (event.message == "Starting in 4 seconds.") {
            testAutoPearl()
        }
        else if (event.message == "[NPC] Mort: Here, I found this map when I first entered the dungeon." && !goOn1) {
            hasRunStarted = true
            testAutoPearl()
        }
        else if (event.message == "Starting in 1 second." && goOn1 && !doTest) {
            Scheduler.schedulePreTickTask(goOn1Delay) {
                hasRunStarted = true
                testAutoPearl()
            }
        }
        else if (doTest && event.message == "Starting in 2 seconds.") {
            Scheduler.schedulePreTickTask(goOn1Delay) {
                hasRunStarted = true
                testAutoPearl()
            }
        }
    }

    fun testAutoPearl() {
        if (mc.thePlayer.posY != 72.0 && mc.thePlayer.posY != 71.0) return modMessage("need to be at y 71/72")
        Scheduler.schedulePreTickTask {
            val pearlCLipNode = PearlClip(mc.thePlayer.positionVector, if (mc.thePlayer.posY == 72.0) 10 else 9)
            pearlCLipNode.run()
            RouteUtils.pearlClip(if (mc.thePlayer.posY == 72.0) 10 else 9, silent)
            waitingForClip = true
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true) //detect after pearlclip
    fun onS08(event: PacketEvent.Receive) {
        if (event.packet !is S08PacketPlayerPosLook || !waitingForClip) return
        waitingForClip = false

        Scheduler.schedulePreTickTask {
            val state = if (LocationUtils.isSinglePlayer) SwapManager.swapFromId(277) else SwapManager.swapFromSBId("ASPECT_OF_THE_VOID")
            if (state == SwapManager.SwapState.UNKNOWN || state == SwapManager.SwapState.TOO_FAST) return@schedulePreTickTask
            setRotation(0f, 90f, silent)

            Scheduler.schedulePreTickTask { repeat(VERTICAL_TP_AMOUNT) { PlayerUtils.airClick() } }

            Scheduler.scheduleLowS08Task(VERTICAL_TP_AMOUNT - 1) {
                Scheduler.schedulePreTickTask {
                    val bloodRoom = Dungeon.Info.uniqueRooms.find { it.name == "Blood" }

                    val coords = if (!hasRunStarted || bloodRoom == null) MIDDLE else bloodRoom.getRealCoords(BLOOD_MIDDLE_COORDS)

                    val yaw = RotationUtils.getYaw(coords.x.toDouble(), coords.z.toDouble())
                    val distance = sqrt((mc.thePlayer.posX - coords.x).pow(2) + (mc.thePlayer.posZ - coords.z).pow(2))
                    val aotvNumber = round(distance * 0.08333333333333333333333333333333).toInt() // 1/12

                    setRotation(yaw, GO_STRAIGHT_ON_AOTV_PITCH, silent)

                    Scheduler.schedulePreTickTask { repeat(aotvNumber) { PlayerUtils.airClick() } }
                }
            }
        }
    }

    @SubscribeEvent
    fun onBloodEnter(event: RoomEnterEvent) {
        if (event.room?.name != "Blood" || !hasRunStarted) return
        if (mc.thePlayer.posY < 40) {
            Scheduler.schedulePreTickTask {
                setRotation(null, -90f, silent)
                Scheduler.schedulePreTickTask {
                    repeat(if (doTest) VERTICAL_TP_AMOUNT - 1 else VERTICAL_TP_AMOUNT) { PlayerUtils.airClick() }

                    SwapManager.swapFromName("pearl")

                    RouteUtils.rightClick()

                    if (doTest) {
                        Scheduler.scheduleLowS08Task(1) {
                            PlayerUtils.airClick()
                            Scheduler.schedulePreTickTask(1) { PlayerUtils.airClick() }
                        }
                    }

                    if (doTest) return@schedulePreTickTask

                    if (noWait) Scheduler.schedulePreTickTask(1) { PlayerUtils.airClick() }

                    else Scheduler.scheduleLowS08Task(VERTICAL_TP_AMOUNT) { PlayerUtils.airClick() }
                }
            }
        }
    }

    fun reset() {
        waitingForClip = false
        hasRunStarted = false
    }

    @SubscribeEvent
    fun onUnload(event: WorldEvent.Unload) {
        reset()
    }

    @SubscribeEvent
    fun onLoad(event: WorldEvent.Load) {
        reset()
    }
}