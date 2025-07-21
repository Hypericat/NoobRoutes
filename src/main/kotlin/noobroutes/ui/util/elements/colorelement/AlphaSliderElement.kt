package noobroutes.ui.util.elements.colorelement

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.clickgui.util.ColorUtil.withAlpha
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants
import noobroutes.utils.render.Color
import noobroutes.utils.render.GradientDirection
import noobroutes.utils.render.circle
import noobroutes.utils.render.drawDynamicTexture
import noobroutes.utils.render.gradientRect
import noobroutes.utils.render.popStencil
import noobroutes.utils.render.stencilRoundedRectangle

class AlphaSliderElement(
    x: Float, y: Float,
    override var elementValue: Color
) : UiElement(x, y), ElementValue<Color> {
    override val elementValueChangeListeners = mutableListOf<(Color) -> Unit>()

    override fun draw() {
        GlStateManager.pushMatrix()
        translate(x, y)
        stencilRoundedRectangle(
            -ColorElementsConstants.COLOR_SLIDER_WIDTH_HALF,
            -ColorElementsConstants.COLOR_SLIDER_HEIGHT_HALF,
            ColorElementsConstants.COLOR_SLIDER_WIDTH,
            ColorElementsConstants.COLOR_SLIDER_HEIGHT,
            ColorElementsConstants.COLOR_SLIDER_CIRCLE_RADIUS
        )
        //the alpha background const values are based off of the png size,
        //it will get cut by the stencil tool to match the actual wanted size
        drawDynamicTexture(
            ColorElementsConstants.ALPHA_BACKGROUND,
            -ColorElementsConstants.ALPHA_BACKGROUND_WIDTH_HALF,
            -ColorElementsConstants.ALPHA_BACKGROUND_HEIGHT_HALF,
            ColorElementsConstants.ALPHA_BACKGROUND_WIDTH,
            ColorElementsConstants.ALPHA_BACKGROUND_HEIGHT
        )
        gradientRect(
            -ColorElementsConstants.COLOR_SLIDER_WIDTH_HALF,
            -ColorElementsConstants.COLOR_SLIDER_HEIGHT_HALF,
            ColorElementsConstants.COLOR_SLIDER_WIDTH,
            ColorElementsConstants.COLOR_SLIDER_HEIGHT,
            Color.Companion.TRANSPARENT,
            elementValue.withAlpha(1f),
            0f,
            GradientDirection.Up
        )
        circle(
            0f,
            -ColorElementsConstants.COLOR_SLIDER_HEIGHT_HALF + ColorElementsConstants.COLOR_SLIDER_HEIGHT - ColorElementsConstants.COLOR_SLIDER_HEIGHT * elementValue.alpha,
            ColorElementsConstants.COLOR_SLIDER_CIRCLE_RADIUS,
            Color.Companion.TRANSPARENT,
            Color.Companion.WHITE,
            ColorElementsConstants.COLOR_SLIDER_CIRCLE_BORDER_THICKNESS
        )
        popStencil()
        GlStateManager.popMatrix()
    }
}