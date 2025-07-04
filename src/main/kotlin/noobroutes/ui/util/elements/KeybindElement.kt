package noobroutes.ui.util.elements

import noobroutes.features.settings.impl.Keybinding
import noobroutes.ui.util.ElementRenderer
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.ColorAnimation
import org.lwjgl.input.Keyboard

class KeybindElement(name: String, initialKey: Keybinding, x: Float, y: Float, w: Float, h: Float) :
    UiElement(name, x, y, w, h), ElementValue<Keybinding> {

    override val elementValueChangeListeners = mutableListOf<(Keybinding) -> Unit>()
    override var elementValue: Keybinding = initialKey
    var listening = false

    private inline val isHovered get() = ElementRenderer.isHoveredKeybind(
        elementValue.key,
        x,
        y,
        1.3f,
        1.3f
    )

    private val colorAnim = ColorAnimation(100)
    override fun draw() {
        drawName()
        ElementRenderer.drawKeybind(
            x + w * 0.9f,
            y + halfHeight,
            1.3f,
            1.3f,
            elementValue.key,
            colorAnim,
            listening
        )
    }



    override fun mouseClicked(mouseButton: Int) {
        if (isHovered) {
            if (colorAnim.start()) listening = !listening
            return
        } else if (listening) {
            setValue(Keybinding(-100 + mouseButton))
            if (colorAnim.start()) listening = false
        }
        return
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (listening) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_BACK) {
                setValue(Keybinding(Keyboard.KEY_NONE))
                if (colorAnim.start()) listening = false
            } else if (keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_RETURN) {
                if (colorAnim.start()) listening = false
            } else {
                setValue(Keybinding(keyCode))
                if (colorAnim.start()) listening = false
            }
            return
        }
        return
    }





}