package noobroutes.ui.util.elements


import net.minecraft.client.renderer.GlStateManager
import noobroutes.features.settings.impl.Keybinding
import noobroutes.ui.ColorPalette
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.utils.render.*
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

class KeybindElement(override var elementValue: Keybinding, x: Float, y: Float, val xScale: Float, val yScale: Float, val alignment: TextAlign = TextAlign.Middle) :
    UiElement(x, y), ElementValue<Keybinding> {

    companion object {
        const val KEYBIND_HEIGHT = 20f
        const val KEYBIND_MINIMUM_WIDTH = 36f
        const val KEYBIND_ADDITION_WIDTH = 9f
        const val HALF_KEYBIND_HEIGHT = KEYBIND_HEIGHT * 0.5f
    }

    override val elementValueChangeListeners = mutableListOf<(Keybinding) -> Unit>()

    var listening = false

    private inline val isHovered get() = isHoveredKeybind(
        elementValue.key,
    )

    private fun isHoveredKeybind(key: Int): Boolean {
        val value = if (key > 0) Keyboard.getKeyName(key) ?: "Err"
        else if (key < 0) Mouse.getButtonName(key + 100)
        else "None"
        val width = (getTextWidth(value, 12f).coerceAtLeast(KEYBIND_MINIMUM_WIDTH) + KEYBIND_ADDITION_WIDTH)

        return isAreaHovered(
            -width * 0.5f,
            -KEYBIND_HEIGHT * 0.5f,
            width,
            KEYBIND_HEIGHT
        )
    }


    private val colorAnimation = ColorAnimation(100)
    override fun draw() {
        val value = if (elementValue.key > 0) Keyboard.getKeyName(elementValue.key) ?: "Err"
        else if (elementValue.key < 0) Mouse.getButtonName(elementValue.key + 100)
        else "None"
        val width = getTextWidth(value, 12f).coerceAtLeast(KEYBIND_MINIMUM_WIDTH) + KEYBIND_ADDITION_WIDTH
        val halfWidth = width * 0.5f
        val xOffset = when (alignment) {
            TextAlign.Left -> 0f
            TextAlign.Middle -> -halfWidth
            TextAlign.Right -> -width
        }

        GlStateManager.pushMatrix()
        translate(x + xOffset, y)
        scale(xScale, yScale)

        roundedRectangle(-halfWidth, -HALF_KEYBIND_HEIGHT, width, KEYBIND_HEIGHT, ColorPalette.buttonColor, 5f)
        if (listening || colorAnimation.isAnimating()) {
            rectangleOutline(
                0f,
                -HALF_KEYBIND_HEIGHT,
                width,
                KEYBIND_HEIGHT,
                colorAnimation.get(ColorPalette.clickGUIColor, ColorPalette.buttonColor, listening),
                5f,
                3f
            )
        }
        text(value, -xOffset, 0, ColorPalette.textColor, 12f, align = TextAlign.Middle)
        GlStateManager.popMatrix()
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (isHovered && mouseButton == 0) {
            if (colorAnimation.start()) listening = !listening
            return true
        } else if (listening) {
            setValue(Keybinding(-100 + mouseButton))
            if (colorAnimation.start()) listening = false
        }
        return false
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (listening) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_BACK) {
                setValue(Keybinding(Keyboard.KEY_NONE))
                if (colorAnimation.start()) listening = false
            } else if (keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_RETURN) {
                if (colorAnimation.start()) listening = false
            } else {
                setValue(Keybinding(keyCode))
                if (colorAnimation.start()) listening = false
            }
            return true
        }
        return false
    }





}