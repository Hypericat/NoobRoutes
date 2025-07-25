package noobroutes.ui.util.elements

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.ColorPalette
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.CubicBezierAnimation
import noobroutes.utils.render.*

class SelectorElement(
    x: Float,
    y: Float,
    val xScale: Float,
    val yScale: Float,
    override var elementValue: Int,
    val options: ArrayList<String>
) : UiElement(x, y), ElementValue<Int> {
    companion object {
        const val SELECTOR_ELEMENT_WIDTH = 150f
        const val SELECTOR_ELEMENT_OPTION_HEIGHT = 20f
        const val SELECTOR_ELEMENT_CUSHIONING = 7f
        const val SELECTOR_ELEMENT_HALF_WIDTH = SELECTOR_ELEMENT_WIDTH * 0.5f
        const val SELECTOR_ELEMENT_HALF_OPTION_HEIGHT = SELECTOR_ELEMENT_OPTION_HEIGHT * 0.5f
    }

    override val elementValueChangeListeners = mutableListOf<(Int) -> Unit>()
    var extended = false
    val openAnimation = CubicBezierAnimation(125L, .4, 0, .2, 1)

    override fun draw() {
        GlStateManager.pushMatrix()
        GlStateManager.translate(x -SELECTOR_ELEMENT_HALF_WIDTH - SELECTOR_ELEMENT_CUSHIONING, y, 1f)
        GlStateManager.scale(xScale, yScale, 1f)
        val height = if (extended || openAnimation.isAnimating()) {
            openAnimation.get(0f, 1f, !extended) * (options.size - 1) * (SELECTOR_ELEMENT_OPTION_HEIGHT + SELECTOR_ELEMENT_CUSHIONING) + SELECTOR_ELEMENT_OPTION_HEIGHT + SELECTOR_ELEMENT_CUSHIONING
        } else SELECTOR_ELEMENT_OPTION_HEIGHT + SELECTOR_ELEMENT_CUSHIONING * 2


        roundedRectangle(
            -SELECTOR_ELEMENT_HALF_WIDTH - SELECTOR_ELEMENT_CUSHIONING,
            -SELECTOR_ELEMENT_HALF_OPTION_HEIGHT - SELECTOR_ELEMENT_CUSHIONING,
            SELECTOR_ELEMENT_WIDTH + SELECTOR_ELEMENT_CUSHIONING * 2f,
            height,
            ColorPalette.elementBackground,
            5f
        )
        text(options[elementValue], 0f, 0f, ColorPalette.textColor, 12f, align = TextAlign.Middle)
        if (extended || openAnimation.isAnimating()) {
            stencilRoundedRectangle(
                -SELECTOR_ELEMENT_HALF_WIDTH - SELECTOR_ELEMENT_CUSHIONING,
                -SELECTOR_ELEMENT_HALF_OPTION_HEIGHT - SELECTOR_ELEMENT_CUSHIONING,
                SELECTOR_ELEMENT_WIDTH + SELECTOR_ELEMENT_CUSHIONING * 2f,
                height - SELECTOR_ELEMENT_CUSHIONING,
                5f
            )
            options.asSequence().forEachIndexed { index, option ->
                roundedRectangle(
                    -SELECTOR_ELEMENT_HALF_WIDTH,
                    (SELECTOR_ELEMENT_OPTION_HEIGHT + SELECTOR_ELEMENT_CUSHIONING * 0.5) * (index + 1) - SELECTOR_ELEMENT_HALF_OPTION_HEIGHT + SELECTOR_ELEMENT_CUSHIONING,
                    SELECTOR_ELEMENT_WIDTH,
                    SELECTOR_ELEMENT_OPTION_HEIGHT,
                    ColorPalette.elementBackground,
                    5f
                )
                text(
                    option,
                    0f,
                    0f + (SELECTOR_ELEMENT_OPTION_HEIGHT + SELECTOR_ELEMENT_CUSHIONING * 0.5) * (index + 1) + SELECTOR_ELEMENT_CUSHIONING,
                    ColorPalette.textColor,
                    12f,
                    align = TextAlign.Middle
                )
            }
            popStencil()
        }

        GlStateManager.popMatrix()
    }

    private inline val isHovered
        get() = MouseUtils.isAreaHovered(
            x - SELECTOR_ELEMENT_WIDTH * 2f,
            y - SELECTOR_ELEMENT_HALF_OPTION_HEIGHT - SELECTOR_ELEMENT_CUSHIONING,
            SELECTOR_ELEMENT_WIDTH,
            SELECTOR_ELEMENT_OPTION_HEIGHT + SELECTOR_ELEMENT_CUSHIONING
        )

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton != 0) return false
        if (isHovered) {
            if (openAnimation.start()) extended = !extended
            return true
        }
        if (extended) {
            if (findHoveredOption()) return true
        }
        return false
    }

    private fun findHoveredOption() : Boolean{
        for (index in 0 until options.size) {
            if (MouseUtils.isAreaHovered(
                    x - SELECTOR_ELEMENT_WIDTH * 2f,
                    y + SELECTOR_ELEMENT_HALF_OPTION_HEIGHT + SELECTOR_ELEMENT_CUSHIONING + (SELECTOR_ELEMENT_OPTION_HEIGHT + SELECTOR_ELEMENT_CUSHIONING * 0.5f) * index,
                    SELECTOR_ELEMENT_WIDTH,
                    SELECTOR_ELEMENT_OPTION_HEIGHT
                )) {
                if (openAnimation.start()) {
                    setValue(index)
                    extended = false
                }
                return true
            }
        }
        return false
    }
}

