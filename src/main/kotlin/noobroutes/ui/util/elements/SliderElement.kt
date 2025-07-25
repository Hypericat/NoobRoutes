package noobroutes.ui.util.elements

import net.minecraft.client.renderer.GlStateManager
import noobroutes.Core.logger
import noobroutes.ui.ColorPalette
import noobroutes.ui.ColorPalette.clickGUIColor
import noobroutes.ui.clickgui.ClickGUI.TEXTOFFSET
import noobroutes.ui.clickgui.util.HoverHandler
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.MouseUtils.mouseX
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.utils.ColorUtil.brighter
import noobroutes.utils.floor
import noobroutes.utils.render.*
import noobroutes.utils.round
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
import kotlin.math.roundToInt

class SliderElement(
    name: String,
    x: Float,
    y: Float,
    val w: Float,
    val h: Float,
    val xScale: Float,
    val yScale: Float,
    override var elementValue: Double,
    val min: Double, val max: Double,
    val increment: Double,
    val unit: String = "",
    val roundTo: Int = 2,
    val divideBy: Double = 1.0
) : UiElement(x, y), ElementValue<Double> {

    override val elementValueChangeListeners = mutableListOf<(Double) -> Unit>()
    private val handler = HoverHandler(0, 0)
    private val colorAnim = ColorAnimation(100)
    private var listeningText = false
    private var listeningTextField: String = ""
    var listening = false

    private inline val isHovered: Boolean
        get() = isAreaHovered(x + TEXTOFFSET, y + SLIDER_VERTICAL_OFFSET - SLIDER_HEIGHT, w - TEXTOFFSET, SLIDER_HEIGHT * 3f)

    private var sliderPercentage: Float = ((elementValue- min) / (max - min)).toFloat()

    private inline val color: Color
        get() = clickGUIColor.brighter(1 + handler.percent() / 200f)


    override fun draw() {
        handler.handle(x + TEXTOFFSET, y + SLIDER_VERTICAL_OFFSET - SLIDER_HEIGHT, w - TEXTOFFSET, SLIDER_HEIGHT * 3f)
        //need to impliment a text element here
        /*
        drawSliderTextBox(
            getDisplay(),
            x + w - TEXTOFFSET,
            y + halfHeight,
            1f,
            1f,
            colorAnim.get(
                ColorPalette.elementPrimary,
                ColorPalette.backgroundSecondary.brighter(1.1f),
                !listening
            ).darkerIf(isHoveredBox)
        )

         */
        drawSlider(x + TEXTOFFSET, SLIDER_VERTICAL_OFFSET, w - TEXTOFFSET, SLIDER_HEIGHT, sliderPercentage, color)
        updateSlider()
        if (listening) {
            val diff = max - min
            val newVal = min + ((mouseX - (x + TEXTOFFSET)) / (w - 15f)).coerceIn(0f, 1f) * diff
            setValue(newVal)
        }
    }

    companion object {
        private val keyWhiteList = listOf(
            Keyboard.KEY_0,
            Keyboard.KEY_1,
            Keyboard.KEY_2,
            Keyboard.KEY_3,
            Keyboard.KEY_4,
            Keyboard.KEY_5,
            Keyboard.KEY_6,
            Keyboard.KEY_7,
            Keyboard.KEY_8,
            Keyboard.KEY_9,
            Keyboard.KEY_NUMPAD0,
            Keyboard.KEY_NUMPAD1,
            Keyboard.KEY_NUMPAD2,
            Keyboard.KEY_NUMPAD3,
            Keyboard.KEY_NUMPAD4,
            Keyboard.KEY_NUMPAD5,
            Keyboard.KEY_NUMPAD6,
            Keyboard.KEY_NUMPAD7,
            Keyboard.KEY_NUMPAD8,
            Keyboard.KEY_NUMPAD9,
            Keyboard.KEY_MINUS
        )
        const val SLIDER_HEIGHT = 7f
        val sliderBGColor = Color(-0xefeff0)
        const val SLIDER_VERTICAL_OFFSET = SLIDER_HEIGHT * 5f

        fun drawSlider(x: Float, y: Float, w: Float, h: Float, percentage: Float, sliderColor: Color) {
            roundedRectangle(x, y, w, h, sliderBGColor, 3f)
            roundedRectangle(x, y, percentage * w, h, sliderColor, 3f)
        }

        const val TEXT_BOX_HEIGHT = 21.5f
        const val HALF_TEXT_BOX_HEIGHT = TEXT_BOX_HEIGHT * 0.5f
        const val SLIDER_TEXT_BOX_PADDING = 9f

        fun drawSliderTextBox(display: String, x: Float, y: Float, xScale: Float, yScale: Float, color: Color) {
            GlStateManager.pushMatrix()
            GlStateManager.translate(x, y, 1f)
            GlStateManager.scale(xScale, yScale, 1f)
            val textWidth = getTextWidth(display, 12f)
            roundedRectangle(-textWidth - SLIDER_TEXT_BOX_PADDING, -HALF_TEXT_BOX_HEIGHT, textWidth + SLIDER_TEXT_BOX_PADDING * 2f, TEXT_BOX_HEIGHT, clickGUIColor, 4f, edgeSoftness = 1f)
            rectangleOutline(-textWidth - SLIDER_TEXT_BOX_PADDING, -HALF_TEXT_BOX_HEIGHT, textWidth + SLIDER_TEXT_BOX_PADDING * 2f, TEXT_BOX_HEIGHT, color,4f, 3f)
            text(display, 0, 0, ColorPalette.textColor, 12f, align =  TextAlign.Right)
            GlStateManager.popMatrix()
        }
    }

    private fun getDisplay(): String {
        if (listeningText) {
            return "${listeningTextField.ifEmpty { " " }}${unit}"
        }
        val divideByMultiplier = 1 / divideBy

        return if (elementValue - elementValue.floor() == 0.0) {
            if (roundTo == 0) {
                "${(elementValue).roundToInt() * divideByMultiplier}${unit}"
            }
            else "${(elementValue).round(roundTo).toDouble() * divideByMultiplier}${unit}"
        } else {
            "${(elementValue).round(roundTo).toDouble() * divideByMultiplier}${unit}"
        }
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton != 0) return false
        when {
            /*
            isHoveredBox -> {
                if (listeningText) {
                    textUnlisten()
                } else {
                    listeningText = true
                    listeningTextField = elementValue.round(roundTo).toString()
                }
                return true
            }

             */
            listeningText -> {
                textUnlisten()
                listeningText = false
                if (isHovered) { listening = true }
                return true
            }
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

    override fun mouseClickedAnywhere(){
        if (listeningText && !isHovered) {
            textUnlisten()
            return
        }
        return
    }

    private fun textUnlisten() {
        if (listeningTextField.isEmpty()) {
            setValue(min)
            updateSlider()
            listeningText = false
            return
        }
        setValue(
            try {
                listeningTextField.toDouble().round(roundTo).toDouble().coerceIn(min, max)
            } catch (e: NumberFormatException) {
                modMessage("Invalid number! Defaulting to previous value.")
                 logger.error(listeningTextField, e)
                elementValue.round(roundTo).toDouble()
            }
        )
        updateSlider()
        listeningText = false
    }

    fun updateSlider() {
        sliderPercentage = ((elementValue - min) / (max - min)).toFloat()
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (listeningText) {
            var text = listeningTextField
            when (keyCode) {
                Keyboard.KEY_ESCAPE, Keyboard.KEY_NUMPADENTER, Keyboard.KEY_RETURN -> {
                    textUnlisten()
                    return true
                }

                Keyboard.KEY_PERIOD -> {
                    if (listeningTextField.contains('.')) return true
                    listeningTextField += '.'
                    return true
                }

                Keyboard.KEY_DELETE -> {
                    listeningTextField = handleText(text.dropLast(1))
                    return true
                }

                Keyboard.KEY_BACK -> {
                    listeningTextField =
                        if (Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
                            ""
                        } else {
                            handleText(text.dropLast(1))
                        }
                    return true
                }

                in keyWhiteList -> {
                    text += typedChar.toString()
                    listeningTextField = handleText(text)
                    return true
                }
            }
        }
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
    private fun handleText(input: String): String {
        val cleaned = buildString {
            input.forEachIndexed { index, char ->
                when {
                    char.isDigit() -> append(char)
                    char == '-' && index == 0 && !contains('-') -> append(char)
                    char == '.' && !contains('.') -> append(char)
                }
            }
        }
        return cleaned
    }


}