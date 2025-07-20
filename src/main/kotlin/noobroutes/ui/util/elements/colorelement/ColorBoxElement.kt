package noobroutes.ui.util.elements.colorelement

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.clickgui.util.ColorUtil.hsbMax
import noobroutes.ui.clickgui.util.ColorUtil.withAlpha
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants
import noobroutes.utils.render.Color
import noobroutes.utils.render.GradientDirection
import noobroutes.utils.render.circle
import noobroutes.utils.render.drawDynamicTexture
import noobroutes.utils.render.drawHSBBox
import noobroutes.utils.render.gradientRect
import noobroutes.utils.render.popStencil
import noobroutes.utils.render.stencilRoundedRectangle

class ColorBoxElement(
    x: Float, y: Float,
    override var elementValue: Color
) : UiElement(x, y), ElementValue<Color> {
    override val elementValueChangeListeners = mutableListOf<(Color) -> Unit>()

    override fun draw() {
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 1f)
        stencilRoundedRectangle(
            -ColorElementsConstants.COLOR_BOX_SIZE_HALF,
            -ColorElementsConstants.COLOR_BOX_SIZE_HALF,
            ColorElementsConstants.COLOR_BOX_SIZE,
            ColorElementsConstants.COLOR_BOX_SIZE,
            ColorElementsConstants.COLOR_BOX_RADIUS
        )
        drawHSBBox(
            -ColorElementsConstants.COLOR_BOX_SIZE_HALF,
            -ColorElementsConstants.COLOR_BOX_SIZE_HALF,
            ColorElementsConstants.COLOR_BOX_SIZE,
            ColorElementsConstants.COLOR_BOX_SIZE,
            elementValue.hsbMax()
        )

        circle(
            elementValue.saturation * ColorElementsConstants.COLOR_SLIDER_WIDTH,
            (1 - elementValue.brightness) * ColorElementsConstants.COLOR_SLIDER_WIDTH,
            ColorElementsConstants.COLOR_BOX_CIRCLE_RADIUS,
            Color.Companion.TRANSPARENT,
            Color.Companion.WHITE,
            ColorElementsConstants.COLOR_BOX_CIRCLE_THICKNESS
        )

        popStencil()
        GlStateManager.popMatrix()
    }
}