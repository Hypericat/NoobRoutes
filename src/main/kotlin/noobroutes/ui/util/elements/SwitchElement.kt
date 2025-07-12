package noobroutes.ui.util.elements

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.ColorPalette
import noobroutes.ui.clickgui.util.ColorUtil.brighter
import noobroutes.ui.clickgui.util.ColorUtil.saturationIf
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.ui.util.animations.impl.LinearAnimation
import noobroutes.utils.render.Color
import noobroutes.utils.render.circle
import noobroutes.utils.render.roundedRectangle

class SwitchElement(
    name: String,
    val scale: Float,
    override var elementValue: Boolean,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    ) : UiElement(name, x, y, w, h), ElementValue<Boolean>  {



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

        fun drawSwitch(x: Float, y: Float, xScale: Float, yScale: Float, enabled: Boolean, linear: LinearAnimation<Float>, colorAnimation: ColorAnimation) {
            val hovered = isHoveredSwitch(x, y, xScale, yScale)

            GlStateManager.pushMatrix()
            GlStateManager.translate(x, y, 1f)
            GlStateManager.scale(xScale, yScale, 1f)

            val backgroundColor = colorAnimation.get(
                ColorPalette.elementPrimary,
                ColorPalette.backgroundPrimary.brighter(1.1f),
                enabled
            ).saturationIf(hovered, 0.75f)

            roundedRectangle(
                -SWITCH_WIDTH_HALF,
                -SWITCH_HEIGHT_HALF,
                SWITCH_WIDTH,
                SWITCH_HEIGHT,
                ColorPalette.backgroundPrimary.brighter(1.2f).saturationIf(hovered, 0.75f), 9f
            )

            if (enabled || linear.isAnimating()) {
                roundedRectangle(-SWITCH_WIDTH_HALF, -SWITCH_HEIGHT_HALF, linear.get(SWITCH_WIDTH, 9f, enabled), SWITCH_HEIGHT, backgroundColor, 9f)
            }

            circle(linear.get(-SWITCH_CIRCLE_START, -SWITCH_WIDTH_HALF, !enabled) + SWITCH_CIRCLE_OFFSET, 0, 6f,
                Color(220, 220, 220)
            )
            GlStateManager.popMatrix()
        }
    }

    override val elementValueChangeListeners = mutableListOf<(Boolean) -> Unit>()
    private val colorAnimation = ColorAnimation(250)
    private val linearAnimation = LinearAnimation<Float>(200)


    private inline val isHovered get() = isHoveredSwitch(
        x + w * 0.9f,
        y + halfHeight,
        1f,
        1f,
    )

    override fun draw() {
        drawName()
        drawSwitch(
            x + w * 0.9f,
            y + halfHeight,
            1.3f,
            1.3f,
            elementValue,
            linearAnimation,
            colorAnimation
        )
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