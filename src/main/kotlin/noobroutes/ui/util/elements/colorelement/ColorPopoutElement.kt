package noobroutes.ui.util.elements.colorelement

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.ColorPalette
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.elements.NumberBoxElement
import noobroutes.ui.util.elements.TextBoxElement
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants.TEXT_BOX_WIDTH
import noobroutes.utils.ColorUtil.darker
import noobroutes.utils.Utils.COLOR_NORMALIZER
import noobroutes.utils.render.Color
import noobroutes.utils.render.Color.Companion.HEX_REGEX
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.roundedRectangle

class ColorPopoutElement(
    x: Float,
    y: Float,
    val alphaEnabled: Boolean = false,
    override var elementValue: Color
) : UiElement(x, y), ElementValue<Color> {
    override val elementValueChangeListeners = mutableListOf<(Color) -> Unit>()

    private val popupWidth = ColorElementsConstants.COLOR_POPOUT_WIDTH + if (alphaEnabled) ColorElementsConstants.COLOR_POPOUT_ALPHA_WIDTH else 0f

    init {
        addChildren(
            listOf("R", "G", "B").mapIndexed { index, label ->
                NumberBoxElement(
                    label, 0f, 0f,
                    TEXT_BOX_WIDTH, ColorElementsConstants.TEXT_BOX_HEIGHT,
                    12f, TextAlign.Left, 5f, 6f,
                    ColorPalette.textColor,
                    3,
                    TextBoxElement.TextBoxType.GAP,
                    0,
                    0.0,
                    255.0,
                    when (index) {
                        0 -> elementValue.r.toDouble()
                        1 -> elementValue.g.toDouble()
                        else -> elementValue.b.toDouble()
                    }
                ).apply {
                    when (index) {
                        0 -> {
                            addValueChangeListener { updateColor(r = it.toInt()) }
                        }

                        1 -> {
                            addValueChangeListener { updateColor(g = it.toInt()) }
                        }

                        2 -> {
                            addValueChangeListener { updateColor(b = it.toInt()) }
                        }

                        else -> {}
                    }
                }
            }
        )
        addChild(
            TextBoxElement(
                "HEX", 0f, 0f,
                popupWidth - ColorElementsConstants.COLOR_POPOUT_GAP * 3f + 3f,
                ColorElementsConstants.TEXT_BOX_HEIGHT,
                12f, TextAlign.Middle, 5f, 6f,
                ColorPalette.textColor, if (alphaEnabled) 8 else 6,
                TextBoxElement.TextBoxType.GAP,
                elementValue.hex,
            ).apply {
                addValueChangeListener {
                    if (!HEX_REGEX.matches(it)) {
                        elementValue = (parent as? ColorPopoutElement)?.elementValue?.hex ?: return@addValueChangeListener
                        return@addValueChangeListener
                    }
                    val r = it.substring(0, 2).toInt(16)
                    val g = it.substring(2, 4).toInt(16)
                    val b = it.substring(4, 6).toInt(16)
                    val a = if (it.length == 8) it.substring(6, 8).toInt(16) * COLOR_NORMALIZER else 1f
                    updateColor(r, g, b, a)
                }
            }
        )


        addChild(ColorBoxElement(0f, 0f, elementValue).apply { addValueChangeListener { parent?.updateChildren() } })
        addChild(ColorSliderElement(0f, 0f, elementValue).apply { addValueChangeListener { parent?.updateChildren() } })
        if (alphaEnabled) addChild(AlphaSliderElement(0f, 0f, elementValue).apply { addValueChangeListener { parent?.updateChildren() } })
    }

    fun updateColor(r: Int? = null, g: Int? = null, b: Int? = null, a: Float? = null, hue: Float? = null, saturation: Float? = null, brightness: Float? = null) {
        r?.let { elementValue.r = it }
        g?.let { elementValue.g = it }
        b?.let { elementValue.b = it }
        a?.let { elementValue.alpha = it }
        hue?.let { elementValue.hue = it }
        saturation?.let { elementValue.saturation = it }
        brightness?.let { elementValue.brightness = it }
        updateChildren()
        invokeValueChangeListeners()
    }

    override fun updateChildren() {
        for (i in 0 until uiChildren.size) {
            val child = uiChildren[i]
            when(i) {
                0 -> (child as NumberBoxElement).elementValue = this.elementValue.r.toDouble()
                1 -> (child as NumberBoxElement).elementValue = this.elementValue.g.toDouble()
                2 -> (child as NumberBoxElement).elementValue = this.elementValue.b.toDouble()
                3 -> (child as TextBoxElement).elementValue = this.elementValue.hex
            }
            child.updateChildren()
        }
    }

    override fun draw() {
        GlStateManager.pushMatrix()
        translate(x, y)
        val width = ColorElementsConstants.COLOR_POPOUT_WIDTH + if (alphaEnabled) ColorElementsConstants.COLOR_POPOUT_ALPHA_WIDTH else 0f
        val topRX = width * -0.5f
        val topRY = ColorElementsConstants.COLOR_POPOUT_HEIGHT * -0.5f
        roundedRectangle(
            topRX,
            topRY,
            width,
            ColorElementsConstants.COLOR_POPOUT_HEIGHT,
            ColorPalette.elementBackground,
            ColorPalette.elementBackground.darker(0.7f),
            Color.Companion.TRANSPARENT,
            5f,
            10f,
            10f,
            10f,
            10f,
            0.5f
        )
        for (i in 0..2) {
            uiChildren[i].updatePosition(
                topRX + ColorElementsConstants.COLOR_POPOUT_GAP * (i + 1) + ColorElementsConstants.TEXT_BOX_WIDTH_WITH_GAP * i,
                topRY + ColorElementsConstants.COLOR_POPOUT_GAP * 2f + ColorElementsConstants.COLOR_BOX_SIZE
            )
        }
        uiChildren[3].updatePosition(
            topRX + ColorElementsConstants.COLOR_POPOUT_GAP,
            topRY + ColorElementsConstants.COLOR_POPOUT_GAP * 3f + ColorElementsConstants.COLOR_BOX_SIZE + ColorElementsConstants.TEXT_BOX_HEIGHT
        )
        uiChildren[4].updatePosition(
            topRX + ColorElementsConstants.COLOR_POPOUT_GAP + ColorElementsConstants.COLOR_BOX_SIZE_HALF,
            topRY + ColorElementsConstants.COLOR_POPOUT_GAP + ColorElementsConstants.COLOR_BOX_SIZE_HALF
        )
        uiChildren[5].updatePosition(
            topRX + ColorElementsConstants.COLOR_BOX_SIZE + ColorElementsConstants.COLOR_POPOUT_GAP * 2f + ColorElementsConstants.COLOR_SLIDER_WIDTH * 0.5f,
            topRY + ColorElementsConstants.COLOR_POPOUT_GAP + ColorElementsConstants.COLOR_BOX_SIZE_HALF
        )
        if (alphaEnabled) {
            uiChildren[6].updatePosition(
                topRX + ColorElementsConstants.COLOR_BOX_SIZE + ColorElementsConstants.COLOR_POPOUT_GAP * 3f + ColorElementsConstants.COLOR_SLIDER_WIDTH * 1.5f,
                topRY + ColorElementsConstants.COLOR_POPOUT_GAP + ColorElementsConstants.COLOR_BOX_SIZE_HALF
            )
        }
        uiChildren.forEach { it.draw() }

        GlStateManager.popMatrix()
    }
}