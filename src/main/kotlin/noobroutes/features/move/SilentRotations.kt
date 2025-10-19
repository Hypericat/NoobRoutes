package noobroutes.features.move

import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.DevOnly
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.utils.SpinnySpinManager
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard

@DevOnly
object SilentRotations: Module(
    name = "Silent Rotations",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "post-prediction silent rotations"
) {
    private val desyncToggle by KeybindSetting("Desync Toggle", Keyboard.KEY_NONE, "Desyncs server and client rotation").onPress { desync() }

    private fun desync() {
        SpinnySpinManager.setDesync(!SpinnySpinManager.getDesyncStatus());
        modMessage((if (SpinnySpinManager.getDesyncStatus()) "Enabled" else "Disabled") + " silent rotation desync.")
    }
}