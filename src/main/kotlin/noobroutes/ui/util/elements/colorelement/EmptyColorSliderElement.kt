package noobroutes.ui.util.elements.colorelement

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants
import noobroutes.utils.render.Color
import noobroutes.utils.render.rectangleOutline

class EmptyColorSliderElement(
    x: Float, y: Float, val color: Color, val thickness: Float
) : UiElement(x, y) {

    override fun draw() {
        GlStateManager.pushMatrix()
        translate(x - ColorElementsConstants.COLOR_SLIDER_WIDTH_HALF, y - ColorElementsConstants.COLOR_SLIDER_HEIGHT_HALF)
        rectangleOutline(
            0f,
            0f,
            ColorElementsConstants.COLOR_SLIDER_WIDTH + 2f,
            ColorElementsConstants.COLOR_SLIDER_HEIGHT,
            color,
            ColorElementsConstants.COLOR_SLIDER_CIRCLE_RADIUS,
            thickness
        )
        GlStateManager.popMatrix()
    }
}