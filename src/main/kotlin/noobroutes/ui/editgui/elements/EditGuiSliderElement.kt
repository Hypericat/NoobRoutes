package noobroutes.ui.editgui.elements

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.ColorPalette.TEXT_OFFSET
import noobroutes.ui.ColorPalette.elementBackground
import noobroutes.ui.ColorPalette.textColor
import noobroutes.ui.clickgui.elements.SettingElement.Companion.BORDER_OFFSET
import noobroutes.ui.clickgui.elements.menu.SettingElementSlider
import noobroutes.ui.editgui.EditGuiBase
import noobroutes.ui.editgui.EditGuiElement
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.elements.SliderElement
import noobroutes.ui.util.elements.textElements.NumberBoxElement
import noobroutes.ui.util.elements.textElements.TextBoxElement
import noobroutes.utils.render.ColorUtil.darker
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.text
import noobroutes.utils.round

class EditGuiSliderElement(
    val name: String,
    min: Double,
    max: Double,
    increment: Double,
    roundTo: Int,
    val getter: () -> Double,
    val setter: (Double) -> Unit
) : UiElement(0f, 0f), EditGuiElement {
    override val priority: Int = 2
    override val isDoubleWidth: Boolean = true
    override val height: Float = 80f

    private inline var value
        get() = getter.invoke()
        set(value) = setter(value)

    companion object {
        private const val TEXT_BOX_HEIGHT = 28.6f
        private const val HALF_TEXT_BOX_HEIGHT = TEXT_BOX_HEIGHT * 0.5f
        private const val SLIDER_HEIGHT = 7f
        private const val Y_PADDING = -2f
        private const val NUMBER_BOX_MIN_WIDTH = 37.333f
        private const val NUMBER_BOX_RADIUS = 6f
        private const val NUMBER_BOX_PADDING = 9f
        private const val TEXT_BOX_THICKNESS = 2f
        private const val BASE_WIDTH = EditGuiBase.WIDTH - 60f
    }

    val sliderElement = SliderElement(
        BORDER_OFFSET,
        32f,
        BASE_WIDTH - BORDER_OFFSET * 2f,
        SLIDER_HEIGHT,
        value,
        min,
        max,
        increment,
        roundTo
    ).apply {
        addValueChangeListener { sliderValue ->
            elementValue = sliderValue
            updateValues(sliderValue)
        }
    }
    val numberBoxElement = NumberBoxElement(
        "",
        BASE_WIDTH - BORDER_OFFSET,
        Y_PADDING - TEXT_BOX_HEIGHT * 0.5f + 6f,
        NUMBER_BOX_MIN_WIDTH,
        TEXT_BOX_HEIGHT,
        16f,
        TextAlign.Right,
        NUMBER_BOX_RADIUS,
        NUMBER_BOX_PADDING,
        textColor.darker(),
        8,
        TextBoxElement.TextBoxType.NORMAL,
        TEXT_BOX_THICKNESS,
        roundTo,
        min,
        max,
        value
    ).apply {
        addValueChangeListener { boxValue ->
            updateValues(boxValue)
        }
    }

    fun updateValues(sliderValue: Double) {
        value = sliderValue
        sliderElement.elementValue = sliderValue
        numberBoxElement.apply {
            elementValue = sliderValue.round(roundTo).toDouble()
            updateTextBoxValue()
        }
    }

    init {
        addChildren(numberBoxElement, sliderElement)
    }

    override fun draw() {
        GlStateManager.pushMatrix()
        translate(x, y)
        text(name, TEXT_OFFSET,Y_PADDING + HALF_TEXT_BOX_HEIGHT * 0.5f, textColor, 16f)
        GlStateManager.popMatrix()
    }

}
