package noobroutes.ui.clickgui.elements.menu

import net.minecraft.client.renderer.GlStateManager
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.ui.ColorPalette
import noobroutes.ui.ColorPalette.TEXT_OFFSET
import noobroutes.ui.ColorPalette.elementBackground
import noobroutes.ui.ColorPalette.textColor
import noobroutes.ui.clickgui.elements.Element
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.ui.util.elements.textElements.NumberBoxElement
import noobroutes.ui.util.elements.SliderElement
import noobroutes.ui.util.elements.textElements.TextBoxElement
import noobroutes.utils.render.ColorUtil.darker
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.getTextHeight
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.text
import noobroutes.utils.round

/**
 * Renders all the modules.
 *
 * Backend made by Aton, with some changes
 * Design mostly made by Stivais
 *
 * @author Stivais, Aton
 * @see [Element]
 */
class ElementSlider(setting: NumberSetting<*>) :
    Element<NumberSetting<*>>(setting, ElementType.SLIDER) {

    companion object {
        private const val TEXT_BOX_HEIGHT = 21.5f
        private const val HALF_TEXT_BOX_HEIGHT = TEXT_BOX_HEIGHT * 0.5f
        private const val SLIDER_TEXT_BOX_PADDING = 9f
        private const val SLIDER_HEIGHT = 7f
        private const val Y_PADDING = 12f
        private const val NUMBER_BOX_MIN_WIDTH = 14f
        private const val NUMBER_BOX_RADIUS = 6f
        private const val NUMBER_BOX_PADDING = 3f
        private const val TEXT_BOX_THICKNESS = 2f
    }

    private val roundTo = if (setting.value is Int) 0 else 2

    val sliderElement = SliderElement(
        BORDER_OFFSET,
        40f,
        w - BORDER_OFFSET * 2,
        SLIDER_HEIGHT,
        setting.valueDouble,
        setting.min,
        setting.max,
        setting.increment,
        if (setting.value is Int) 0 else 2
    ).apply {
        addValueChangeListener { sliderValue ->
            setting.setValueFromNumber(sliderValue)
            updateValues(sliderValue)
        }
    }
    val numberBoxElement = NumberBoxElement(
        "",
        w - BORDER_OFFSET,
        Y_PADDING - TEXT_BOX_HEIGHT * 0.5f + 6f,
        NUMBER_BOX_MIN_WIDTH,
        TEXT_BOX_HEIGHT,
        12f,
        TextAlign.Right,
        NUMBER_BOX_RADIUS,
        NUMBER_BOX_PADDING,
        textColor.darker(),
        8,
        TextBoxElement.TextBoxType.NORMAL,
        TEXT_BOX_THICKNESS,
        roundTo,
        setting.min,
        setting.max,
        setting.valueDouble
    ).apply {
        addValueChangeListener { boxValue ->
            setting.setValueFromNumber(boxValue)
            updateValues(boxValue)
        }
    }

    init {
        addChildren(numberBoxElement, sliderElement)
    }

    fun updateValues(sliderValue: Double) {
        sliderElement.elementValue = sliderValue
        numberBoxElement.apply {
            elementValue = setting.valueDouble.round(roundTo).toDouble()
            updateTextBoxValue()
        }
    }

    override fun draw() {
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)
        roundedRectangle(0f, 0f, w, h, elementBackground)
        roundedRectangle(0f, 0f, w, h, elementBackground)

        text(name, TEXT_OFFSET, Y_PADDING + HALF_TEXT_BOX_HEIGHT * 0.5f, textColor, 12f)
        GlStateManager.popMatrix()
    }
}