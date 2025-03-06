package me.odinmain.ui.clickgui.elements.menu

import me.odinmain.OdinMain.logger
import me.odinmain.features.settings.impl.NumberSetting
import me.odinmain.font.OdinFont
import me.odinmain.ui.clickgui.ClickGUI.TEXTOFFSET
import me.odinmain.ui.clickgui.animations.impl.ColorAnimation
import me.odinmain.ui.clickgui.elements.Element
import me.odinmain.ui.clickgui.elements.ElementType
import me.odinmain.ui.clickgui.elements.ModuleButton
import me.odinmain.ui.clickgui.elements.menu.ElementTextField.Companion.keyBlackList
import me.odinmain.ui.clickgui.util.ColorUtil.brighter
import me.odinmain.ui.clickgui.util.ColorUtil.clickGUIColor
import me.odinmain.ui.clickgui.util.ColorUtil.darkerIf
import me.odinmain.ui.clickgui.util.ColorUtil.elementBackground
import me.odinmain.ui.clickgui.util.ColorUtil.textColor
import me.odinmain.ui.clickgui.util.HoverHandler
import me.odinmain.ui.util.MouseUtils.isAreaHovered
import me.odinmain.ui.util.MouseUtils.mouseX
import me.odinmain.utils.floor
import me.odinmain.utils.render.*
import me.odinmain.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
import kotlin.math.roundToInt

/**
 * Renders all the modules.
 *
 * Backend made by Aton, with some changes
 * Design mostly made by Stivais
 *
 * @author Stivais, Aton
 * @see [Element]
 */
class ElementSlider(parent: ModuleButton, setting: NumberSetting<*>) :
    Element<NumberSetting<*>>(parent, setting, ElementType.SLIDER) {
        private var listeningText = false
//55
    override val isHovered: Boolean
        get() = isAreaHovered(x, y + 21.5f, w - 15f, 33.5f)

    private val isHoveredBox: Boolean
        get() = isAreaHovered(x + w - TEXTOFFSET - 30, y  + 5f, 32f, 21.5f )

    private val handler = HoverHandler(0, 150)
    private val colorAnim = ColorAnimation(100)

    /** Used to make slider smoother and not jittery (doesn't change value.) */
    private var sliderPercentage: Float = ((setting.valueDouble - setting.min) / (setting.max - setting.min)).toFloat()

    private inline val color: Color
        get() = clickGUIColor.brighter(1 + handler.percent() / 200f)

    private fun getDisplay(): String {
        if (listeningText) {
            return listeningTextField.ifEmpty { " " }
        }
        return if (setting.valueDouble - setting.valueDouble.floor() == 0.0) {
            "${(setting.valueInt * 100.0).roundToInt() / 100}${setting.unit}"
        } else {
            "${(setting.valueDouble * 100.0).roundToInt() / 100.0}${setting.unit}"
        }
    }

    override fun draw() {
        handler.handle(x, y + 21.5f, w - 15f, 33.5f)
        val percentage = ((setting.valueDouble - setting.min) / (setting.max - setting.min)).toFloat()
        rectangleOutline(x + w - TEXTOFFSET - 30, y  + 5f, 32f, 21.5f, colorAnim.get(textColor.darkerIf(isHoveredBox, 0.8f), clickGUIColor.darkerIf(isHoveredBox, 0.8f), !listeningText), 4f, 3f)

        if (listening) {
            sliderPercentage = ((mouseX - (x + TEXTOFFSET)) / (w - 15f)).coerceIn(0f, 1f)
            val diff = setting.max - setting.min
            val newVal = setting.min + ((mouseX - (x + TEXTOFFSET)) / (w - 15f)).coerceIn(0f, 1f) * diff
            setting.valueDouble = newVal
        }
        roundedRectangle(x, y, w, h, elementBackground)
        roundedRectangle(x, y, w, h, elementBackground)
        roundedRectangle(x + w - 2, y, 2, h, clickGUIColor, 0f, edgeSoftness = 0)

        text(name, x + TEXTOFFSET, y + h / 2f - 10f, textColor, 12f, OdinFont.REGULAR)
        text(getDisplay(), x + w - TEXTOFFSET, y + h / 2f - 10f, textColor.darkerIf(isHoveredBox), 12f, OdinFont.REGULAR, TextAlign.Right)

        //draw slider
        roundedRectangle(x + TEXTOFFSET, y + 37f, w - 17f, 7f, sliderBGColor, 2.5f)
        roundedRectangle(x + TEXTOFFSET, y + 37f, sliderPercentage * (w - 17f), 7f, color, 3f)
        circle(x + TEXTOFFSET + sliderPercentage * (w - 17f), y + 37f + 3f, 5f, color)

    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (isHoveredBox && mouseButton == 0) {
            modMessage("clicked box")
            listeningText = true
            listeningTextField = setting.valueInt.toString()
            return true
        }
        if (listeningText) {
            textUnlisten()
            listeningText = false
            return false
        }

        if (mouseButton == 0 && isHovered) {
            listening = true
            return true
        }
        return false
    }

    override fun mouseReleased(state: Int) {
        listening = false
    }

    private fun textUnlisten() {
        if (listeningTextField.isEmpty()) {
            setting.valueDouble = setting.min
            return
        }
        setting.valueDouble = try {
            listeningTextField.toDouble()
        } catch (e: NumberFormatException) {
            modMessage("Invalid number! Defaulting to previous value.")
            logger.error(listeningTextField, e)
            setting.valueDouble
        }
        sliderPercentage = ((setting.valueDouble - setting.min) / (setting.max - setting.min)).toFloat()
        listeningText = false
    }

    private fun handleText(number: String):String {
        if (number.isNotEmpty() && number.last() == '.' && number.count { it == '.' } >= 2) {
            return number.dropLast(1)
        }
        return number
    }
    var listeningTextField: String = ""

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        modMessage(listeningText)
        if (listeningText) {
            if (listeningTextField.length >= 3) return true
            var text = listeningTextField
            when (keyCode) {
                Keyboard.KEY_ESCAPE, Keyboard.KEY_NUMPADENTER, Keyboard.KEY_RETURN -> {
                    textUnlisten()
                    return true
                }
                Keyboard.KEY_BACK -> {
                    listeningTextField = handleText(text.dropLast(1))
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
                Keyboard.KEY_RIGHT -> setting.increment
                Keyboard.KEY_LEFT -> -setting.increment
                else -> return false
            }
            setting.valueDouble += amount
            return true
        }
        return false
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
        Keyboard.KEY_9
    )
}