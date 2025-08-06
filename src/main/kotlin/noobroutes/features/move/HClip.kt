package noobroutes.features.move

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.render.FreeCam
import noobroutes.features.settings.DevOnly
import noobroutes.features.settings.NotPersistent
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.Scheduler
import noobroutes.utils.Utils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
import kotlin.math.atan2

@NotPersistent
object HClip: Module(
    name = "HClip",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "hclips when activating"
) {

    private val omni by BooleanSetting("omni", false, description = "should go in a direction based of key inputs")
    private val shouldSpam by BooleanSetting("should repeat", false, description = "should repeatedly hclip if holding the button")
    private val hclipInterval by NumberSetting(name = "delay", description = "how long to wait between hclips", min = 2, max = 10, default = 6).withDependency { shouldSpam }

    private var since = 0

    override fun onKeybind() {
        if (!FreeCam.enabled) toggle()
    }

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (mc.currentScreen != null) {
            modMessage("Gui Open")
            toggle()
        }
        if (!shouldSpam) return
        if (event.phase != TickEvent.Phase.START) return
        since++
        if (since == hclipInterval) {
            since = 0
            hclip()
        }
    }

    @SubscribeEvent
    fun onKey(event: InputEvent.KeyInputEvent) {
        val key = Keyboard.getEventKey()
        if (key != this.keybinding?.key) return
        if (!Keyboard.getEventKeyState()) {
            since = 0
            toggle()
        }
    }

    private fun hclip() {
        if (mc.thePlayer == null) return
        mc.thePlayer.motionX = 0.0
        mc.thePlayer.motionZ = 0.0
        PlayerUtils.unPressKeys()
        Scheduler.scheduleC03Task {
            val speed = PlayerUtils.getPlayerWalkSpeed() * 2.806
            val renderEntity = mc.renderViewEntity
            mc.thePlayer.motionX = speed * Utils.xPart(renderEntity.rotationYawHead + yawChange())
            mc.thePlayer.motionZ = speed * Utils.zPart(renderEntity.rotationYawHead + yawChange())
            PlayerUtils.rePressKeys()
        }
        if (!shouldSpam) toggle()
    }

    override fun onEnable() {
        super.onEnable()
        hclip()
    }

    fun yawChange(): Int {
        if (!omni) return 0
        val deltaX = (if (Keyboard.isKeyDown(mc.gameSettings.keyBindRight.keyCode)) 1 else 0) + (if (Keyboard.isKeyDown(mc.gameSettings.keyBindLeft.keyCode)) -1 else 0)
        val deltaZ = (if (Keyboard.isKeyDown(mc.gameSettings.keyBindForward.keyCode)) 1 else 0) + (if (Keyboard.isKeyDown(mc.gameSettings.keyBindBack.keyCode)) -1 else 0)

        if (deltaX == 0 && deltaZ == 0) return 0

        return Math.toDegrees(atan2(deltaX.toDouble(), deltaZ.toDouble())).toInt()
    }


}