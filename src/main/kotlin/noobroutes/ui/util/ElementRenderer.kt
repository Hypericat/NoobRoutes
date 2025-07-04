package noobroutes.ui.util

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.ColorPalette
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.ui.util.animations.impl.LinearAnimation
import noobroutes.ui.clickgui.util.ColorUtil.brighter
import noobroutes.ui.clickgui.util.ColorUtil.saturationIf
import noobroutes.utils.render.*
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

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

    fun drawKeybind(x: Float, y: Float, xScale: Float, yScale: Float, key: Int, colorAnimation: ColorAnimation, listening: Boolean){
        val value = if (key > 0) Keyboard.getKeyName(key) ?: "Err"
        else if (key < 0) Mouse.getButtonName(key + 100)
        else "None"
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 1f)
        GlStateManager.scale(xScale, yScale, 1f)
        val width = getTextWidth(value, 12f).coerceAtLeast(12f) + 9f
        val height = getTextHeight(value, 12f) + 6f
        roundedRectangle(width * -0.5f, height * -0.5f, width, height, ColorPalette.elementSecondary, 5f)
        text(value, 0, 0, ColorPalette.text, 12f, align = TextAlign.Middle)
        GlStateManager.popMatrix()
    }

    fun isHoveredKeybind(key: Int, x: Float, y: Float, xScale: Float, yScale: Float): Boolean {
        val value = if (key > 0) Keyboard.getKeyName(key) ?: "Err"
        else if (key < 0) Mouse.getButtonName(key + 100)
        else "None"
        val width = (getTextWidth(value, 12f).coerceAtLeast(12f) + 9) * xScale
        val height = (getTextHeight(value, 12f) + 6f) * yScale
        return MouseUtils.isAreaHovered(
            x - width * 0.5f,
            y - height * 0.5f,
            width,
            height
        )
    }


}