package noobroutes.ui.util.elements

import noobroutes.font.FontType
import noobroutes.ui.ColorPalette
import noobroutes.ui.util.animations.impl.LinearAnimation
import noobroutes.ui.util.ElementRenderer
import noobroutes.ui.util.ElementRenderer.TEXT_OFFSET
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.ui.util.shader.GlowShader
import noobroutes.ui.util.shader.GlowShader2D
import noobroutes.utils.render.Color
import noobroutes.utils.render.text

class SwitchElement(
    val name: String,
    initialValue: Boolean,
    x: Float,
    y: Float,
    w: Float,
    h: Float
) : UiElement<Boolean>(x, y, w, h) {
    override var elementValue: Boolean = initialValue

    private val colorAnimation = ColorAnimation(250)
    private val linearAnimation = LinearAnimation<Float>(200)
    //200
    //50
    private val xScale = w / 200f
    private val yScale = h / 50f



    private inline val isHovered get() = ElementRenderer.isHoveredSwitch(
        x + w * 0.9f,
        y + halfHeight,
        xScale,
        yScale,
    )




    override fun draw() {
        drawBackground()
        //GlowShader2D.startDraw()

        GlowShader.startDraw()
        text(name, x + TEXT_OFFSET, y + h * 0.5f, ColorPalette.text, 16f, fontType = ColorPalette.font)
        GlowShader.endDraw(Color.ORANGE, 6f, 2f)
        //GlowShader2D.stopDraw(Color.ORANGE, 6f, 2f)
        ElementRenderer.drawSwitch(
            x + w * 0.9f,
            y + halfHeight,
            xScale,
            yScale,
            elementValue,
            linearAnimation,
            colorAnimation
        )
    }

    override fun mouseLeftClicked() {
        if (!isHovered) return
        if (colorAnimation.start()) {
            linearAnimation.start()
            setValue(!elementValue)
        }
    }





}