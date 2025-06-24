package noobroutes.features.move

import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.impl.NumberSetting
import org.lwjgl.input.Keyboard

object BHop: Module(
    name = "BHop",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "applies a speed boost when u sprint jump"
) {
    val speed by NumberSetting(name = "boost", description = "how much speed it should give", min = 0.2f, max = 4f, default = 2f, increment = 0.05)
}