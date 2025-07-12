package noobroutes.ui.util.elements

import net.minecraft.client.renderer.GlStateManager
import noobroutes.Core.logger
import noobroutes.ui.ColorPalette
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.Animation
import noobroutes.ui.util.animations.impl.LinearAnimation
import noobroutes.ui.util.elements.ElementSelector.Companion.SELECTOR_ELEMENT_CUSHIONING
import noobroutes.ui.util.elements.ElementSelector.Companion.SELECTOR_ELEMENT_HALF_OPTION_HEIGHT
import noobroutes.ui.util.elements.ElementSelector.Companion.SELECTOR_ELEMENT_OPTION_HEIGHT
import noobroutes.ui.util.elements.ElementSelector.Companion.SELECTOR_ELEMENT_WIDTH
import noobroutes.utils.render.Color
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.rectangleOutline
import noobroutes.utils.render.resetStencil
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.stencilRoundedRectangle
import noobroutes.utils.render.text
import kotlin.math.E

class ElementSelector(
    name: String,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    override var elementValue: Int,
    val options: ArrayList<String>
) : UiElement(name, x, y, w, h), ElementValue<Int> {
    companion object {
        const val SELECTOR_ELEMENT_WIDTH = 150f
        const val SELECTOR_ELEMENT_OPTION_HEIGHT = 20f
        const val SELECTOR_ELEMENT_CUSHIONING = 7f
        const val SELECTOR_ELEMENT_HALF_WIDTH = SELECTOR_ELEMENT_WIDTH * 0.5f
        const val SELECTOR_ELEMENT_HALF_OPTION_HEIGHT = SELECTOR_ELEMENT_OPTION_HEIGHT * 0.5f


        fun drawSelector(
            x: Float,
            y: Float,
            xScale: Float,
            yScale: Float,
            elementValue: Int,
            extended: Boolean,
            options: ArrayList<String>,
            openAnimation: Animation<Float>
        ) {
            GlStateManager.pushMatrix()
            GlStateManager.translate(x, y, 1f)
            GlStateManager.scale(xScale, yScale, 1f)
            val height = if (extended || openAnimation.isAnimating()) {
                openAnimation.get(0f, 1f, !extended) * (options.size - 1) * (SELECTOR_ELEMENT_OPTION_HEIGHT + SELECTOR_ELEMENT_CUSHIONING) + SELECTOR_ELEMENT_OPTION_HEIGHT + SELECTOR_ELEMENT_CUSHIONING
            } else SELECTOR_ELEMENT_OPTION_HEIGHT + SELECTOR_ELEMENT_CUSHIONING * 2


            roundedRectangle(
                -SELECTOR_ELEMENT_HALF_WIDTH - SELECTOR_ELEMENT_CUSHIONING,
                -SELECTOR_ELEMENT_HALF_OPTION_HEIGHT - SELECTOR_ELEMENT_CUSHIONING,
                SELECTOR_ELEMENT_WIDTH + SELECTOR_ELEMENT_CUSHIONING * 2f,
                height,
                ColorPalette.backgroundSecondary,
                5f
            )
            text(options[elementValue], 0f, 0f, ColorPalette.text, 12f, align = TextAlign.Middle)
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
                        ColorPalette.elementSecondary,
                        5f
                    )
                    text(
                        option,
                        0f,
                        0f + (SELECTOR_ELEMENT_OPTION_HEIGHT + SELECTOR_ELEMENT_CUSHIONING * 0.5) * (index + 1) + SELECTOR_ELEMENT_CUSHIONING,
                        ColorPalette.text,
                        12f,
                        align = TextAlign.Middle
                    )
                }
                resetStencil()
            }

            GlStateManager.popMatrix()
        }
    }

    override val elementValueChangeListeners = mutableListOf<(Int) -> Unit>()
    var extended = false
    val openAnimation = LinearAnimation<Float>(125)

    override fun draw() {
        drawName()
        drawSelector(
            x + w - SELECTOR_ELEMENT_WIDTH - ColorPalette.TEXT_OFFSET - SELECTOR_ELEMENT_HALF_WIDTH - SELECTOR_ELEMENT_CUSHIONING,
            y + halfHeight,
            1f,
            1f,
            elementValue,
            extended,
            options,
            openAnimation
        )
        //logger.info(extended)
    }

    private inline val isHovered
        get() = MouseUtils.isAreaHovered(
            x + w - SELECTOR_ELEMENT_WIDTH * 2f - ColorPalette.TEXT_OFFSET,
            y + halfHeight - SELECTOR_ELEMENT_HALF_OPTION_HEIGHT - SELECTOR_ELEMENT_CUSHIONING,
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
                    x + w - SELECTOR_ELEMENT_WIDTH * 2f - ColorPalette.TEXT_OFFSET,
                    y + halfHeight + SELECTOR_ELEMENT_HALF_OPTION_HEIGHT + SELECTOR_ELEMENT_CUSHIONING + (SELECTOR_ELEMENT_OPTION_HEIGHT + SELECTOR_ELEMENT_CUSHIONING * 0.5f) * index,
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

