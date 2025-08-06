package noobroutes.ui.util.elements.colorelement

import net.minecraft.client.renderer.GlStateManager
import noobroutes.Core.logger
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants
import noobroutes.utils.render.*
import noobroutes.utils.render.ColorUtil.hsbMax

class ColorSliderElement(
    x: Float, y: Float,
    override var elementValue: Color
) : UiElement(x, y), ElementValue<Color> {
    override val elementValueChangeListeners = mutableListOf<(Color) -> Unit>()

    override fun draw() {
        GlStateManager.pushMatrix()
        translate(x, y)
        GlStateManager.translate(0f, 0f, 1f)
        circle(
            0f,
            -ColorElementsConstants.COLOR_SLIDER_HEIGHT_HALF + ColorElementsConstants.COLOR_SLIDER_HEIGHT * (1 - elementValue.hue),
            ColorElementsConstants.COLOR_SLIDER_CIRCLE_RADIUS,
            elementValue.hsbMax(),
            Color.Companion.WHITE,
            ColorElementsConstants.COLOR_SLIDER_CIRCLE_BORDER_THICKNESS
        )
        GlStateManager.translate(0f, 0f, -1f)

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

        popStencil()




        if (dragging) {
            elementValue.hue = getMouseYPercentageInBounds(
                -ColorElementsConstants.COLOR_SLIDER_HEIGHT_HALF,
                ColorElementsConstants.COLOR_SLIDER_HEIGHT, true
            )
            invokeValueChangeListeners()
        }
        GlStateManager.popMatrix()
    }


    private inline val isHovered get() = isAreaHovered(
        -ColorElementsConstants.COLOR_SLIDER_WIDTH_HALF,
        -ColorElementsConstants.COLOR_SLIDER_HEIGHT_HALF,
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