package noobroutes.features.move

import net.minecraft.network.play.client.C0APacketAnimation
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.PacketUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard

object QOL: Module(
    name = "QOL",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "some random stuff"
) {
    val jumpDelay by NumberSetting(name = "Jump Delay", description = "when holding jump the delay beetween jumps (10 is default)", min = 0, max = 10, default = 0, increment = 1)
    val lavaFix by BooleanSetting("Lava Fix", false, description = "the shit from sby")
    val noRot by BooleanSetting("No Rotate", false, description = "do not rotate on any teleports")
    val altMovement by BooleanSetting("Alt Movement", false, description = "Change how some movement things behave")
    val instantSpeed by BooleanSetting("Instant Speed", false, description = "Instant Speed when a movement key is pressed, also causes omnisprint").withDependency { altMovement }
    val instantStop by BooleanSetting("Instant Stop", false , description = "stop immediately if no movement keys are pressed").withDependency { altMovement }
    val bhopToggle by BooleanSetting("Bhop", false, description = "gives bigger jump speed boost").withDependency { altMovement }
    val bhopSpeed by NumberSetting(name = "Bhop speed", description = "how big the boost should be", min = 6.0, max = 6.2, default = 6.0, increment = 0.01).withDependency { altMovement }

    private val leftClickSpam by KeybindSetting("Spam Left", Keyboard.KEY_NONE, "clicks left click a few times").onPress { spamLeft() }
    private val spamLeftAmount by NumberSetting("Spam Left Amount", 20, 5, 40, description = "how often it should left click")

    private val rightClickSpam by KeybindSetting("Spam Right", Keyboard.KEY_NONE, "clicks left click a few times").onPress { spamRight() }
    private val spamRightAmount by NumberSetting("Spam Right Amount", 20, 5, 40, description = "how often it should right click")

    private fun spamLeft() {
        modMessage("boom")
        repeat(spamLeftAmount) {
            PacketUtils.sendPacket(C0APacketAnimation())
            devMessage("sent")
        }
    }

    private fun spamRight() {
        modMessage("woosh")
        repeat(spamRightAmount) {
            PlayerUtils.airClick()
            devMessage("sent")
        }
    }
}