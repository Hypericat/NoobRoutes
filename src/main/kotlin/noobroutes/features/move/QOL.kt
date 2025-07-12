package noobroutes.features.move

import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.NumberSetting
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
}