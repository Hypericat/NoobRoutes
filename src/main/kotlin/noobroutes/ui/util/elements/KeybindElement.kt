package noobroutes.ui.util.elements


import net.minecraft.client.renderer.GlStateManager
import noobroutes.features.settings.impl.Keybinding
import noobroutes.ui.ColorPalette
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.utils.render.*
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

class KeybindElement(name: String, override var elementValue: Keybinding, x: Float, y: Float, w: Float, h: Float) :
    UiElement(name, x, y, w, h), ElementValue<Keybinding> {

    companion object {
        const val KEYBIND_HEIGHT = 20f
        const val KEYBIND_MINIMUM_WIDTH = 36f
        const val KEYBIND_ADDITION_WIDTH = 9f
        const val HALF_KEYBIND_HEIGHT = KEYBIND_HEIGHT * 0.5f

        fun drawKeybind(x: Float, y: Float, xScale: Float, yScale: Float, key: Int, colorAnimation: ColorAnimation, listening: Boolean){
            val value = if (key > 0) Keyboard.getKeyName(key) ?: "Err"
            else if (key < 0) Mouse.getButtonName(key + 100)
            else "None"
            GlStateManager.pushMatrix()
            GlStateManager.translate(x, y, 1f)
            GlStateManager.scale(xScale, yScale, 1f)
            val width = getTextWidth(value, 12f).coerceAtLeast(KEYBIND_MINIMUM_WIDTH) + KEYBIND_ADDITION_WIDTH
            val halfWidth = width * 0.5f

            roundedRectangle(-halfWidth, -HALF_KEYBIND_HEIGHT, width, KEYBIND_HEIGHT, ColorPalette.elementSecondary, 5f)
            if (listening || colorAnimation.isAnimating()) {
                rectangleOutline(
                    -halfWidth,
                    -HALF_KEYBIND_HEIGHT,
                    width,
                    KEYBIND_HEIGHT,
                    colorAnimation.get(ColorPalette.elementSecondary, ColorPalette.elementPrimary, listening),
                    5f,
                    3f
                )
            }
            text(value, 0, 0, ColorPalette.text, 12f, align = TextAlign.Middle)
            GlStateManager.popMatrix()
        }

        fun isHoveredKeybind(key: Int, x: Float, y: Float, xScale: Float, yScale: Float): Boolean {
            val value = if (key > 0) Keyboard.getKeyName(key) ?: "Err"
            else if (key < 0) Mouse.getButtonName(key + 100)
            else "None"
            val width = (getTextWidth(value, 12f).coerceAtLeast(KEYBIND_MINIMUM_WIDTH) + KEYBIND_ADDITION_WIDTH) * xScale

            return MouseUtils.isAreaHovered(
                x - width * 0.5f,
                y - KEYBIND_HEIGHT * 0.5f,
                width,
                KEYBIND_HEIGHT
            )
        }
    }

    override val elementValueChangeListeners = mutableListOf<(Keybinding) -> Unit>()

    var listening = false

    private inline val isHovered get() = isHoveredKeybind(
        elementValue.key,
        x + w * 0.9f,
        y + halfHeight,
        1.3f,
        1.3f
    )

    private val colorAnim = ColorAnimation(100)
    override fun draw() {
        drawName()
        colorAnim.isAnimating()
        drawKeybind(
            x + w * 0.9f,
            y + halfHeight,
            1.3f,
            1.3f,
            elementValue.key,
            colorAnim,
            listening
        )

    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (isHovered && mouseButton == 0) {
            if (colorAnim.start()) listening = !listening
            return true
        } else if (listening) {
            setValue(Keybinding(-100 + mouseButton))
            if (colorAnim.start()) listening = false
        }
        return false
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
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
            return true
        }
        return false
    }





}