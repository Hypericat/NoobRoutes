package noobroutes.ui.util.elements.colorelement

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.ColorPalette
import noobroutes.ui.clickgui.util.ColorUtil.hsbMax
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants
import noobroutes.utils.render.*

class ColorBoxElement(
    x: Float, y: Float,
    override var elementValue: Color
) : UiElement(x, y), ElementValue<Color> {
    override val elementValueChangeListeners = mutableListOf<(Color) -> Unit>()

    override fun draw() {
        GlStateManager.pushMatrix()
        translate(x - ColorElementsConstants.COLOR_BOX_SIZE_HALF, y - ColorElementsConstants.COLOR_BOX_SIZE_HALF)
        stencilRoundedRectangle(
            0f,
            0f,
            ColorElementsConstants.COLOR_BOX_SIZE,
            ColorElementsConstants.COLOR_BOX_SIZE,
            ColorElementsConstants.COLOR_BOX_RADIUS
        )
        drawHSBBox(
            0f,
            0f,
            ColorElementsConstants.COLOR_BOX_SIZE,
            ColorElementsConstants.COLOR_BOX_SIZE,
            elementValue.hsbMax()
        )

        circle(
            elementValue.saturation * ColorElementsConstants.COLOR_BOX_SIZE,
            (1 - elementValue.brightness) * ColorElementsConstants.COLOR_BOX_SIZE,
            ColorElementsConstants.COLOR_BOX_CIRCLE_RADIUS,
            Color.Companion.TRANSPARENT,
            Color.Companion.WHITE,
            ColorElementsConstants.COLOR_BOX_CIRCLE_THICKNESS
        )

        popStencil()
        GlStateManager.popMatrix()
        if (dragging) {
            elementValue.saturation =
                ((MouseUtils.mouseX - x - xOrigin + ColorElementsConstants.COLOR_BOX_SIZE_HALF) / ColorElementsConstants.COLOR_BOX_SIZE).coerceIn(
                    0f,
                    1f
                )
            elementValue.brightness =
                ((ColorElementsConstants.COLOR_BOX_SIZE - (MouseUtils.mouseY - y - yOrigin + ColorElementsConstants.COLOR_BOX_SIZE_HALF) ) / ColorElementsConstants.COLOR_BOX_SIZE).coerceIn(
                    0f,
                    1f
                )
            invokeValueChangeListeners()
        }

    }

    var dragging = false
    val isHovered get() = isAreaHovered(
        -ColorElementsConstants.COLOR_BOX_SIZE_HALF + x,
        -ColorElementsConstants.COLOR_BOX_SIZE_HALF + y,
        ColorElementsConstants.COLOR_BOX_SIZE,
        ColorElementsConstants.COLOR_BOX_SIZE,
    )
    override fun mouseClicked(mouseButton: Int): Boolean {
        if (super.mouseClicked(mouseButton)) return true
        if (mouseButton != 0) return false
        if (isHovered) {
            dragging = true
            return true
        }
        return false
    }

    override fun mouseReleased(): Boolean {
        dragging = false
        return super.mouseReleased()
    }
}