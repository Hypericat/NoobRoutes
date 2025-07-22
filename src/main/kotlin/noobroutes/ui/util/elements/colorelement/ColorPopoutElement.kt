package noobroutes.ui.util.elements.colorelement

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.ColorPalette
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.elements.NumberBoxElement
import noobroutes.ui.util.elements.TextBoxElement
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants.TEXT_BOX_WIDTH
import noobroutes.utils.render.Color
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
                    ColorPalette.text,
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
                )
            }
        )
        addChild(
            TextBoxElement(
                "HEX", 0f, 0f,
                popupWidth - ColorElementsConstants.COLOR_POPOUT_GAP * 3f + 3f,
                ColorElementsConstants.TEXT_BOX_HEIGHT,
                12f, TextAlign.Middle, 5f, 6f,
                ColorPalette.text, 3,
                TextBoxElement.TextBoxType.GAP,
                elementValue.hex,
            )
        )


        addChild(ColorBoxElement(0f, 0f, elementValue))
        addChild(ColorSliderElement(0f, 0f, elementValue))
        if (alphaEnabled) addChild(AlphaSliderElement(0f, 0f, elementValue))
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
            ColorPalette.backgroundPrimary,
            ColorPalette.backgroundSecondary,
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