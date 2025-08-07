package noobroutes.features.move

import net.minecraft.network.play.client.C0APacketAnimation
import noobroutes.features.Category
import noobroutes.features.Module
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
    val speed by NumberSetting(name = "bhop speed", description = "when u jump how big the speed boost should be (0.2 is default)", min = 0.2f, max = 4f, default = 2f, increment = 0.05)
    val jumpDelay by NumberSetting(name = "jump delay", description = "when holding jump the delay beetween jumps (10 is default)", min = 0, max = 10, default = 0, increment = 1)
    val lavaFix by BooleanSetting("lava fix", false, description = "the shit from sby")

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