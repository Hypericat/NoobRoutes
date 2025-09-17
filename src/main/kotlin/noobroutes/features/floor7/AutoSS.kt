package noobroutes.features.floor7


import net.minecraft.block.Block
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.ClientChatReceivedEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.Core.logger
import noobroutes.events.impl.BlockChangeEvent
import noobroutes.events.impl.ChatPacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.render.ClickGUIModule.devMode
import noobroutes.features.settings.DevOnly
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.PacketUtils
import noobroutes.utils.RotationUtils.getYawAndPitch
import noobroutes.utils.clock.Executor
import noobroutes.utils.clock.Executor.Companion.register
import noobroutes.utils.render.Color
import noobroutes.utils.render.RenderUtils.drawStringInWorld
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.sendCommand
import org.lwjgl.input.Keyboard

/**
 * taken from cga
 *
 * @Author Kaze.0707
 *
 * **/


object AutoSS : Module(
    name = "AutoSS",
    category = Category.FLOOR7,
    description = "Does SS, (stolen from Kaze)"
){
    private val clickDelay by NumberSetting("Delay", 200.0, 50.0, 500.0, 10.0, unit = "ms", description = "The delay for next click")
    private val forceDevice by BooleanSetting("Force Device", false, description = "").withDependency {devMode}
    private val resetSSKeybind by KeybindSetting("Reset SS", Keyboard.KEY_NONE, "Resets AutoSS on press").onPress { resetKey() }

    @DevOnly
    private val i1Keybind by KeybindSetting("i1 test", Keyboard.KEY_NONE, "tries to i1 on press").onPress { tryI1() }
    @DevOnly
    private val i1ClickAmount by NumberSetting("i1 clicks", 24, 9, 36, description = "how often i1 should click")
    @DevOnly
    private val i1Delay by NumberSetting("i1 delay", 42, 5, 60, description = "how long to wait beetween click bursts")
    @DevOnly
    private val i1People by NumberSetting("i1 fake people", 2, 0, 5, description = "how many extra people it should click for (so it would be click this + 1 many times, wait delay, repeat")

    private val autoStart by BooleanSetting("Autostart", true, description = "Automatically starts autoSS")
    private val autoStartDelay by NumberSetting("Autostart delay", 125.0, 50.0, 200.0, 1.0, unit = "ms", description = "The delay used for starting autoSS")
    private val dontCheck by BooleanSetting("Faster SS?", false, description = "idk what this means")
    private val sendSSBroke by BooleanSetting("Send SS Broke", description = "If The player hits the restart SS Keybind")

    init {
        ssLoop()
    }


    var lastClickAdded = System.currentTimeMillis()
    var next = false
    var progress = 0
    var doneFirst = false
    var doingSS = false
    var clicked = false
    var clicks = ArrayList<BlockPos>()
    var wtflip = System.currentTimeMillis()
    var clickedButton: Vec3? = null
    var allButtons = ArrayList<Vec3>()

    fun reset() {
        allButtons.clear()
        clicks.clear()
        next = false
        progress = 0
        doneFirst = false
        doingSS = false
        clicked = false
        devMessage("Reset!")
    }


    private fun tryI1() {
        val startButton: BlockPos = BlockPos(110, 121, 91)
        if (mc.thePlayer.getDistanceSqToCenter(startButton) > 25) return modMessage("too far")


        Thread{
            var i1ClicksLeft = i1ClickAmount

            while (i1ClicksLeft > 2) {
                repeat(i1People) {clickStartButton()}
                Thread.sleep(i1Delay.toLong())
                i1ClicksLeft -= 3
            }
            if (i1ClicksLeft != 0) modMessage("smth went very wrong")
        }.start()
    }

    fun clickStartButton() {
        clickButton(110, 121, 91)
    }

    private fun resetKey(){
        sendCommand("pc SS Broke")
        start()
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        reset()
    }

    fun start() {
        allButtons.clear()
        val startButton: BlockPos = BlockPos(110, 121, 91)
        val (yaw, pitch) = getYawAndPitch(110.875, 121.5, 91.5)
        if (mc.thePlayer.getDistanceSqToCenter(startButton) > 25) return
        if (!clicked) {
            devMessage("Starting SS")
            devMessage(System.currentTimeMillis())
            reset()
            clicked = true
            doingSS = true
            Thread{
                try {
                    for (i in 0 until 2) {
                        reset()
                        clickButton(startButton.x, startButton.y, startButton.z)
                        Thread.sleep(autoStartDelay.toLong())
                    }
                    doingSS = true
                    clickButton(startButton.x, startButton.y, startButton.z)
                } catch (e: Exception) {
                    devMessage("Error Occurred")
                    logger.error(e)
                }
            }.start()
        }
    }

    @SubscribeEvent
    fun onChat(event: ClientChatReceivedEvent) {
        val msg = event.message.unformattedText
        val startButton: BlockPos = BlockPos(110, 121, 91)
        if (mc.thePlayer.getDistanceSqToCenter(startButton) > 25) return
        if (msg.contains("Device")) {
            devMessage(System.currentTimeMillis())
        }
        if (!msg.contains("Who dares trespass into my domain")) return
        devMessage("Starting SS")
        start()
    }

    private fun ssLoop() {
        Executor(10) {
            if (System.currentTimeMillis() - lastClickAdded + 1 < clickDelay) return@Executor
            if (mc.theWorld == null) return@Executor
            if (!enabled) return@Executor
            if (!LocationUtils.isInSkyblock && !forceDevice) return@Executor
            val detect: Block = mc.theWorld.getBlockState(BlockPos(110, 123, 92)).block
            val startButton: BlockPos = BlockPos(110, 121, 91)

            if (mc.thePlayer.getDistanceSqToCenter(startButton) > 25) return@Executor

            var device = false

            mc.theWorld.loadedEntityList
                .filterIsInstance<EntityArmorStand>()
                .filter { it.getDistanceToEntity(mc.thePlayer) < 6 && it.displayName.unformattedText.contains("Device") }
                .forEach { _ ->
                    device = true
                }

            if (forceDevice) device = true

            if (!device) {
                clicked = false
                return@Executor
            }

            if ((detect == Blocks.stone_button || (dontCheck && doneFirst)) && doingSS) {
                if (!doneFirst && clicks.size == 3) {
                    clicks.removeAt(0)
                    allButtons.removeAt(0)
                }
                doneFirst = true
                if (progress < clicks.size) {
                    val next: BlockPos = clicks[progress]
                    if (mc.theWorld.getBlockState(next).block == Blocks.stone_button) {
                        clickButton(next.x, next.y, next.z)
                        progress++
                    }
                }
            }
        }.register()
    }

    @SubscribeEvent
    fun onChat(event: ChatPacketEvent){
        if (event.message == "[BOSS] Goldor: Who dares trespass into my domain?" && autoStart) start()
    }



    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (!this.enabled) return
        if (!LocationUtils.isInSkyblock && !forceDevice) return
        if (mc.theWorld == null) return

        val startButton: BlockPos = BlockPos(110, 121, 91)

        if (System.currentTimeMillis() - lastClickAdded > clickDelay) clickedButton = null

        if (mc.thePlayer.getDistanceSqToCenter(startButton) < 1600) {
            if (clickedButton != null) {
               Renderer.drawBlock(BlockPos(clickedButton!!.xCoord, clickedButton!!.yCoord, clickedButton!!.zCoord), Color.GREEN)
            }
            allButtons.forEachIndexed{index, location ->
                drawStringInWorld((index + 1).toString(), Vec3(location.xCoord - 0.0625, location.yCoord + 0.5625, location.zCoord + 0.5), scale = 0.02f, shadow = true, depthTest = false)
            }
        }
    }

    /*@SubscribeEvent
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val mop: MovingObjectPosition = mc.objectMouseOver ?: return
        if (System.currentTimeMillis() - wtflip < 1000) return
        wtflip = System.currentTimeMillis()
        val startButton: BlockPos = BlockPos(110, 121, 91)
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && startButton == event.pos && startButton == mop.blockPos && event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) {
            clicked = false
            reset()
            start()
        }
    }*/

    @SubscribeEvent
    fun onBlockChange(event: BlockChangeEvent) {
        if (event.pos.x == 111 && event.pos.y >= 120 && event.pos.y <= 123 && event.pos.z >= 92 && event.pos.z <= 95) {
            val button: BlockPos = BlockPos(110, event.pos.y, event.pos.z)
            if (event.update.block == Blocks.sea_lantern) {
                if (clicks.size == 2) {
                    if (clicks[0] == button && !doneFirst) {
                        doneFirst = true
                        clicks.removeFirst()
                        allButtons.removeFirst()
                    }
                }
                if (!clicks.contains(button)) {
                    devMessage("Added to clicks: x: ${event.pos.x}, y: ${event.pos.y}, z: ${event.pos.z}")
                    progress = 0
                    clicks.add(button)
                    allButtons.add(Vec3(event.pos.x.toDouble(), event.pos.y.toDouble(), event.pos.z.toDouble()))
                }
            }
        }
    }

    private fun clickButton(x: Int, y: Int, z: Int) {
        if (mc.thePlayer.getDistanceSqToCenter(BlockPos(x, y, z)) > 25) return
        devMessage("Clicked at: x: ${x}, y: ${y}, z: ${z}. Time: ${System.currentTimeMillis()}")
        clickedButton = Vec3(x.toDouble(), y.toDouble(), z.toDouble())
        lastClickAdded = System.currentTimeMillis()
        PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(BlockPos(x, y, z), 4, mc.thePlayer.heldItem, 0.875f, 0.5f, 0.5f))
    }
}