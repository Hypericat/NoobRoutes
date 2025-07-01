package noobroutes.ui.util

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.ColorPalette
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.ui.util.animations.impl.LinearAnimation
import noobroutes.ui.clickgui.util.ColorUtil.brighter
import noobroutes.ui.clickgui.util.ColorUtil.brighterIf
import noobroutes.ui.clickgui.util.ColorUtil.darker
import noobroutes.ui.clickgui.util.ColorUtil.darkerIf
import noobroutes.utils.render.*

object ElementRenderer {
    const val TEXT_OFFSET = 9f

    private const val SWITCH_WIDTH = 34f
    private const val SWITCH_HEIGHT = 20f
    private const val SWITCH_WIDTH_HALF = SWITCH_WIDTH * 0.5f
    private const val SWITCH_HEIGHT_HALF = SWITCH_HEIGHT * 0.5f
    private const val SWITCH_CIRCLE_START = SWITCH_WIDTH * 0.9705882f
    private const val SWITCH_CIRCLE_OFFSET = SWITCH_WIDTH * 0.7209302f


    fun isHoveredSwitch(x: Float, y: Float, xScale: Float, yScale: Float): Boolean {
        val height = SWITCH_HEIGHT * yScale
        val width = SWITCH_WIDTH * xScale
        return MouseUtils.isAreaHovered(x - width * 0.5f, y - height * 0.5f, width, height)
    }

    fun drawSwitch(x: Float, y: Float, xScale: Float, yScale: Float, enabled: Boolean, linear: LinearAnimation<Float>, colorAnimation: ColorAnimation) {

        val hovered = isHoveredSwitch(x, y, xScale, yScale)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 1f)
        GlStateManager.scale(xScale, yScale, 1f)

        val color = colorAnimation.get(
            ColorPalette.primary.darkerIf(hovered, 0.7f),
            ColorPalette.background.brighter(1.2f).brighterIf(hovered, 1.3f),
            enabled
        )
        val backgroundColor = colorAnimation.get(
            ColorPalette.primary,
            ColorPalette.background.brighter(1.2f),
            enabled
        )

        roundedRectangle(-SWITCH_WIDTH_HALF, -SWITCH_HEIGHT_HALF, SWITCH_WIDTH, SWITCH_HEIGHT, ColorPalette.background.brighter(1.2f), 9f)

        if (enabled || linear.isAnimating()) {
            roundedRectangle(-SWITCH_WIDTH_HALF, -SWITCH_HEIGHT_HALF, linear.get(SWITCH_WIDTH, 9f, enabled), SWITCH_HEIGHT, backgroundColor, 9f)
        }



        if (hovered) rectangleOutline(-SWITCH_WIDTH_HALF, -SWITCH_HEIGHT_HALF, SWITCH_WIDTH, SWITCH_HEIGHT, color.darker(.85f), 9f, 3f)
        circle(linear.get(-SWITCH_CIRCLE_START, -SWITCH_WIDTH_HALF, !enabled) + SWITCH_CIRCLE_OFFSET, 0, 6f,
            Color(220, 220, 220).darkerIf(hovered, 0.9f)
        )
        GlStateManager.popMatrix()
    }


}