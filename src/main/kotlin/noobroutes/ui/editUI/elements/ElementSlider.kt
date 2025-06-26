package noobroutes.ui.editUI.elements


import noobroutes.ui.clickgui.ClickGUI.TEXTOFFSET
import noobroutes.ui.clickgui.animations.impl.ColorAnimation
import noobroutes.ui.clickgui.util.ColorUtil.brighter
import noobroutes.ui.clickgui.util.ColorUtil.buttonColor
import noobroutes.ui.clickgui.util.ColorUtil.clickGUIColor
import noobroutes.ui.clickgui.util.ColorUtil.darkerIf
import noobroutes.ui.clickgui.util.ColorUtil.textColor
import noobroutes.ui.clickgui.util.HoverHandler
import noobroutes.ui.editUI.EditUI
import noobroutes.ui.editUI.Element
import noobroutes.ui.util.MouseUtils.isAreaHovered
import noobroutes.ui.util.MouseUtils.mouseX
import noobroutes.utils.render.*
import noobroutes.utils.round
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard


class ElementSlider(
    val name: String,
    val min: Double,
    val max: Double,
    val unit: String,
    private val increment: Double,
    override val getter: () -> Double,
    override val setter: (Double) -> Unit,
    x: Float,
    y: Float,
    val w: Float,
    val h: Float,
    val round: Int
) : Element<Double>(x, y) {


    private var listeningText = false
    private var listening: Boolean = false
    private val valueDouble: Double get() = getter()

    //55
    private val isHovered: Boolean
        get() = isAreaHovered(EditUI.originX + x, EditUI.originY +y + 21.5f, w - 15f, 33.5f)

    private val isHoveredBox: Boolean
        get() = isAreaHovered(
            EditUI.originX + x + w - TEXTOFFSET - 30 - getTextWidth(getDisplay(), 16f),
            EditUI.originY + y + 5f,
            16f + getTextWidth(getDisplay(), 16f),
            21.5f
        )

    private val handler = HoverHandler(0, 150)
    private val colorAnim = ColorAnimation(100)

    /** Used to make slider smoother and not jittery (doesn't change value.) */
    private var sliderPercentage: Float = ((valueDouble.round(round).toDouble() - min) / (max - min)).toFloat().coerceAtMost(1f)

    private inline val color: Color
        get() = clickGUIColor.brighter(1 + handler.percent() / 200f)


    private fun getDisplay(): String {
        if (listeningText) {
            return "${listeningTextField.ifEmpty { " " }}${unit}"
        }
        return "${valueDouble.round(round)}${unit}"
    }

    override fun draw(x: Float, y: Float) {
        handler.handle(x, y + 21.5f, w - 15f, 33.5f)
        val textWidth = getTextWidth(getDisplay(), 16f)

        roundedRectangle(
            x + w - TEXTOFFSET - 30 - textWidth,
            y,
            16f + textWidth,
            26.5f,
            buttonColor,
            4f,
            edgeSoftness = 1f
        )
        rectangleOutline(
            x + w - TEXTOFFSET - 30 - textWidth,
            y,
            16f + textWidth,
            26.5f,
            colorAnim.get(
                buttonColor.darkerIf(isHoveredBox, 0.8f),
                clickGUIColor.darkerIf(isHoveredBox, 0.8f),
                !listeningText
            ),
            4f,
            3f
        )

        if (listening) {
            sliderPercentage = ((mouseX + 10.6f - (x + TEXTOFFSET)) / (w - 15f)).coerceIn(0f, 1f)
            val diff = max - min
            val newVal = min + ((mouseX + 10.6f - (x + TEXTOFFSET)) / (w - 15f)).coerceIn(0f, 1f) * diff
            setter(newVal)
        }
        //roundedRectangle(x + w - 4, y, 2, h, clickGUIColor.brighter(1.6f), 0f, edgeSoftness = 0)

        text(name, x + TEXTOFFSET, y + 17.75f, textColor, 20f)
        text(
            getDisplay(),
            x + w - TEXTOFFSET - 22 - textWidth,
            y + 15.75f,
            textColor.darkerIf(isHoveredBox),
            16f
        )

        //draw slider
        roundedRectangle(x + TEXTOFFSET, y + 37f, w - 30f, 7f, sliderBGColor, 3f)
        roundedRectangle(x + TEXTOFFSET, y + 37f, sliderPercentage * (w - 30f), 7f, color, 3f)

    }

    override fun mouseClicked() {
        when {
            isHoveredBox -> {
                if (listeningText) {
                    textUnlisten()
                } else {
                    listeningText = true
                    listeningTextField = valueDouble.round(round).toString()
                }
            }
            listeningText -> {
                textUnlisten()
                listeningText = false
                if (isHovered) { listening = true }
            }
            isHovered -> listening = true
        }
    }

    override fun mouseReleased() {
        listening = false
    }

    fun updateSlider() {
        sliderPercentage = ((valueDouble.round(round).toDouble() - min) / (max - min)).toFloat().coerceAtMost(1f)
    }


    private fun textUnlisten() {
        val input = listeningTextField.trimEnd('.')
        val newValue = if (input.isEmpty()) min else {
            val double = input.toDoubleOrNull()
            if (double == null) {
                modMessage("Invalid Number! Defaulting to previous value")
                valueDouble.round(round).toDouble()
            } else double
        }
        setter(newValue)
        updateSlider()
        listeningText = false
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

    private var listeningTextField: String = ""

    override fun mouseClickedAnywhere(mouseButton: Int): Boolean {
        if (mouseButton == 0 && listeningText && !isHovered && !isHoveredBox) {
            textUnlisten()
            return true
        }
        return false
    }


    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (listeningText) {
            var text = listeningTextField
            when (keyCode) {
                Keyboard.KEY_ESCAPE, Keyboard.KEY_NUMPADENTER, Keyboard.KEY_RETURN -> {
                    textUnlisten()
                    return
                }

                Keyboard.KEY_PERIOD -> {
                    if (listeningTextField.contains('.')) return
                    listeningTextField += '.'
                    return
                }

                Keyboard.KEY_DELETE -> {
                    listeningTextField = handleText(text.dropLast(1))
                    return
                }

                Keyboard.KEY_BACK -> {
                    listeningTextField =
                        if (Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
                            ""
                        } else {
                            handleText(text.dropLast(1))
                        }
                    return
                }

                in keyWhiteList -> {
                    text += typedChar.toString()
                    listeningTextField = handleText(text)
                    return
                }
            }
        }
        if (isHovered || isHoveredBox) {
            val amount = when (keyCode) {
                Keyboard.KEY_RIGHT -> increment
                Keyboard.KEY_LEFT -> -increment
                else -> return
            }
            setter(amount + valueDouble.round(round).toDouble())
            return
        }
    }

    private companion object {
        val sliderBGColor = Color(-0xefeff0)
    }

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
}