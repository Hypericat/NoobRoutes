package noobroutes.ui.util.elements.colorelement

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants
import noobroutes.utils.render.ColorUtil.withAlpha
import noobroutes.utils.render.*

class AlphaSliderElement(
    x: Float, y: Float,
    override var elementValue: Color
) : UiElement(x, y), ElementValue<Color> {
    override val elementValueChangeListeners = mutableListOf<(Color) -> Unit>()

    override fun draw() {
        GlStateManager.pushMatrix()
        translate(x - ColorElementsConstants.COLOR_SLIDER_WIDTH_HALF, y - ColorElementsConstants.COLOR_SLIDER_HEIGHT_HALF)
        GlStateManager.translate(0f, 0f, 1f)
        circle(
            ColorElementsConstants.COLOR_SLIDER_WIDTH_HALF,
            ColorElementsConstants.COLOR_SLIDER_HEIGHT * (1f - elementValue.alpha),
            ColorElementsConstants.COLOR_SLIDER_CIRCLE_RADIUS,
            elementValue,
            Color.Companion.WHITE,
            ColorElementsConstants.COLOR_SLIDER_CIRCLE_BORDER_THICKNESS
        )
        GlStateManager.translate(0f, 0f, -1f)

        stencilRoundedRectangle(
            0f,
            0f,
            ColorElementsConstants.COLOR_SLIDER_WIDTH,
            ColorElementsConstants.COLOR_SLIDER_HEIGHT,
            ColorElementsConstants.COLOR_SLIDER_CIRCLE_RADIUS
        )


        //the alpha background const values are based off of the png size,
        //it will get cut by the stencil tool to match the actual wanted size
        drawDynamicTexture(
            ColorElementsConstants.ALPHA_BACKGROUND,
            0f,
            0f,
            ColorElementsConstants.ALPHA_BACKGROUND_WIDTH,
            ColorElementsConstants.ALPHA_BACKGROUND_HEIGHT
        )
        gradientRect(
            0f,
            0f,
            ColorElementsConstants.COLOR_SLIDER_WIDTH,
            ColorElementsConstants.COLOR_SLIDER_HEIGHT,
            Color.Companion.TRANSPARENT,
            elementValue.withAlpha(1f),
            0f,
            GradientDirection.Up
        )
        popStencil()
        GlStateManager.translate(0f, 0f, 1f)


        if (dragging) {
            elementValue.alpha = getMouseYPercentageInBounds(
                0f,
                ColorElementsConstants.COLOR_SLIDER_HEIGHT,
                true
            )
            invokeValueChangeListeners()
        }
        GlStateManager.popMatrix()
    }

    private inline val isHovered get() = isAreaHovered(
        0f,
        0f,
        ColorElementsConstants.COLOR_SLIDER_WIDTH,
        ColorElementsConstants.COLOR_SLIDER_HEIGHT,
    )
    var dragging: Boolean = false

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton != 0) return false
        if (isHovered) {
            dragging = true
            return true
        }
        return false
    }

    override fun mouseReleased(): Boolean {
        dragging = false
        return false
    }
}