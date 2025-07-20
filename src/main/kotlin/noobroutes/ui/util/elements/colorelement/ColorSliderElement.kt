package noobroutes.ui.util.elements.colorelement

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants
import noobroutes.utils.render.Color
import noobroutes.utils.render.circle
import noobroutes.utils.render.drawDynamicTexture
import noobroutes.utils.render.popStencil
import noobroutes.utils.render.stencilRoundedRectangle

class ColorSliderElement(
    x: Float, y: Float,
    override var elementValue: Color
) : UiElement(x, y), ElementValue<Color> {
    override val elementValueChangeListeners = mutableListOf<(Color) -> Unit>()

    override fun draw() {
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)
        stencilRoundedRectangle(
            -ColorElementsConstants.COLOR_SLIDER_WIDTH_HALF,
            -ColorElementsConstants.COLOR_SLIDER_HEIGHT_HALF,
            ColorElementsConstants.COLOR_SLIDER_WIDTH,
            ColorElementsConstants.COLOR_SLIDER_HEIGHT,
            ColorElementsConstants.COLOR_SLIDER_CIRCLE_RADIUS
        )
        //rotates 270 to account for the png being horizontal
        GlStateManager.rotate(270f, 0f, 0f, 1f)
        drawDynamicTexture(
            ColorElementsConstants.HUE_GRADIENT,
            -ColorElementsConstants.COLOR_SLIDER_HEIGHT_HALF,
            -ColorElementsConstants.COLOR_SLIDER_WIDTH_HALF,
            ColorElementsConstants.COLOR_SLIDER_HEIGHT,
            ColorElementsConstants.COLOR_SLIDER_WIDTH
        )
        GlStateManager.rotate(-270f, 0f, 0f, -1f)
        circle(
            0f,
            -ColorElementsConstants.COLOR_SLIDER_HEIGHT_HALF + ColorElementsConstants.COLOR_SLIDER_HEIGHT * elementValue.hue,
            ColorElementsConstants.COLOR_SLIDER_CIRCLE_RADIUS,
            Color.Companion.TRANSPARENT,
            Color.Companion.WHITE,
            ColorElementsConstants.COLOR_SLIDER_CIRCLE_BORDER_THICKNESS
        )
        popStencil()
        GlStateManager.popMatrix()
    }

}