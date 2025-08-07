package noobroutes.ui.util.elements

import noobroutes.ui.ColorPalette.clickGUIColor
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.UiElement
import noobroutes.utils.render.Color
import noobroutes.utils.render.ColorUtil.brighterIf
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.round
import org.lwjgl.input.Keyboard

class SliderElement(
    x: Float,
    y: Float,
    val w: Float,
    val h: Float,
    override var elementValue: Double,
    val min: Double, val max: Double,
    val increment: Double,
    val roundTo: Int = 2
) : UiElement(x, y), ElementValue<Double> {

    override val elementValueChangeListeners = mutableListOf<(Double) -> Unit>()
    var listening = false

    private inline val isHovered: Boolean
        get() = isAreaHovered(0f, 0f, w, h)


    private var sliderPercentage: Float = ((elementValue- min) / (max - min)).toFloat()

    private inline val color: Color
        get() = clickGUIColor.brighterIf(isHovered)


    override fun draw() {
        translate(x, y)
        roundedRectangle(0f, 0f, w, h, sliderBGColor, 3f)
        roundedRectangle(0f, 0f, sliderPercentage.coerceIn(0f, 1f) * w, h, color, 3f)
        updateSlider()
        if (listening) {
            val diff = max - min
            val newVal = min + getMouseXPercentageInBounds(0f, w) * diff
            setValue(newVal)
        }
    }

    companion object {
        val sliderBGColor = Color(-0xefeff0)
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton != 0) return false
        when {
            isHovered -> {
                listening = true
                return true
            }
        }
        return false
    }

    override fun mouseReleased(): Boolean {
        listening = false
        return false
    }

    fun updateSlider() {
        sliderPercentage = ((elementValue - min) / (max - min)).toFloat()
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (isHovered) {
            val amount = when (keyCode) {
                Keyboard.KEY_RIGHT -> increment
                Keyboard.KEY_LEFT -> -increment
                else -> return true
            }
            setValue((amount + elementValue.round(roundTo).toDouble()).coerceIn(min, max))
            return true
        }
        return false
    }

}