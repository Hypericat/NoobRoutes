package noobroutes.ui.util.elements

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.ColorPalette
import noobroutes.ui.ColorPalette.buttonColor
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.ui.util.animations.impl.LinearAnimation
import noobroutes.utils.ColorUtil.darkerIf
import noobroutes.utils.ColorUtil.saturationIf
import noobroutes.utils.render.Color
import noobroutes.utils.render.circle
import noobroutes.utils.render.roundedRectangle


/**
 * Drawn from the center
 */
class SwitchElement(
    name: String,
    val scale: Float,
    override var elementValue: Boolean,
    x: Float,
    y: Float,
    val xScale: Float,
    val yScale: Float,
    ) : UiElement(x, y), ElementValue<Boolean>  {



    companion object {
        private const val SWITCH_WIDTH = 34f
        private const val SWITCH_HEIGHT = 20f
        private const val SWITCH_WIDTH_HALF = SWITCH_WIDTH * 0.5f
        private const val SWITCH_HEIGHT_HALF = SWITCH_HEIGHT * 0.5f
        private const val SWITCH_CIRCLE_START = SWITCH_WIDTH * 0.9705882f
        private const val SWITCH_CIRCLE_OFFSET = SWITCH_WIDTH * 0.7209302f


        fun isHoveredSwitch(x: Float, y: Float, xScale: Float, yScale: Float): Boolean {
            val height = SWITCH_HEIGHT * yScale
            val width = (SWITCH_WIDTH + 5f) * xScale
            return MouseUtils.isAreaHovered(x - width * 0.5f, y - height * 0.5f, width, height)
        }

    }

    override val elementValueChangeListeners = mutableListOf<(Boolean) -> Unit>()
    private val colorAnimation = ColorAnimation(250)
    private val linearAnimation = LinearAnimation<Float>(200)


    private inline val isHovered get() = isHoveredSwitch(
        x,
        y,
        1f,
        1f,
    )

    override fun draw() {
        GlStateManager.pushMatrix()
        translate(x, y)
        scale(xScale, yScale)

        val backgroundColor = colorAnimation.get(
            ColorPalette.clickGUIColor,
            ColorPalette.buttonColor,
            elementValue
        ).saturationIf(isHovered, 0.75f)

        roundedRectangle(
            -SWITCH_WIDTH_HALF,
            -SWITCH_HEIGHT_HALF,
            SWITCH_WIDTH,
            SWITCH_HEIGHT,
            buttonColor,
            9f
        )

        if (elementValue || linearAnimation.isAnimating()) {
            roundedRectangle(-SWITCH_WIDTH_HALF, -SWITCH_HEIGHT_HALF, linearAnimation.get(SWITCH_WIDTH, 9f, elementValue), SWITCH_HEIGHT, backgroundColor, 9f)
        }

        circle(linearAnimation.get(-SWITCH_CIRCLE_START, -SWITCH_WIDTH_HALF, !elementValue) + SWITCH_CIRCLE_OFFSET, 0, 6f,
            Color(220, 220, 220).darkerIf(isHovered, 0.9f)
        )
        GlStateManager.popMatrix()
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (!isHovered || mouseButton != 0) return false
        if (colorAnimation.start()) {
            linearAnimation.start()
            setValue(!elementValue)
            return true
        }
        return false

    }
}