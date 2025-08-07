package noobroutes.ui.util.elements.textElements

import noobroutes.Core
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.UiElement
import noobroutes.utils.render.Color
import noobroutes.utils.render.TextAlign
import noobroutes.utils.round
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard

class NumberBoxElement(
    val name: String,
    x: Float,
    y: Float,
    minWidth: Float,
    var h: Float,
    textScale: Float,
    textAlign: TextAlign,
    val radius: Float,
    textPadding: Float,
    boxColor: Color,
    maxCharacters: Int,
    boxType: TextBoxElement.TextBoxType,
    boxThickness: Float = 3f,
    val roundTo: Int,
    val min: Double,
    val max: Double,
    override var elementValue: Double,
) : UiElement(x, y), ElementValue<Double> {
    override val elementValueChangeListeners = mutableListOf<(Double) -> Unit>()

    val textBox = TextBoxElement(
        name,
        x,
        y,
        minWidth,
        h,
        textScale,
        textAlign,
        radius,
        textPadding,
        boxColor,
        maxCharacters,
        boxType,
        boxThickness,
        if (roundTo == 0) this.elementValue.toInt().toString() else this.elementValue.round(roundTo).toString(),
        numberKeyWhiteList,
        ""
    ).apply {
        addValueChangeListener {
            textUnlisten(it)
        }
    }

    init {
        addChild(
            textBox
        )
    }

    companion object {
        val numberKeyWhiteList = listOf(
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
            Keyboard.KEY_MINUS,
            Keyboard.KEY_PERIOD
        )
    }
    private fun textUnlisten(text: String) {
        if (text.isEmpty()) {
            setValue(min)
            updateTextBoxValue()
            return
        }
        setValue(
            try {
                text.toDouble().round(roundTo).toDouble()
            } catch (e: NumberFormatException) {
                modMessage("Invalid number! Defaulting to previous value.")
                Core.logger.error(text, e)
                elementValue
            }
        )
        updateTextBoxValue()
    }
    fun updateTextBoxValue() {
        textBox.elementValue = if (roundTo == 0) this.elementValue.toInt().toString() else this.elementValue.round(roundTo).toString()
    }

    override fun updateChildren() {
        updateTextBoxValue()
    }

}