package noobroutes.ui.util.elements

import noobroutes.font.Font
import noobroutes.font.FontType
import noobroutes.font.fonts.OdinFont
import noobroutes.ui.ColorPalette
import noobroutes.ui.clickgui.util.ColorUtil.brighter
import noobroutes.ui.clickgui.util.ColorUtil.brighterIf
import noobroutes.ui.clickgui.util.ColorUtil.darkerIf
import noobroutes.ui.util.animations.impl.LinearAnimation
import noobroutes.ui.util.ElementRenderer
import noobroutes.ui.util.ElementRenderer.TEXT_OFFSET
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.ui.util.shader.GlowShader
import noobroutes.ui.util.shader.GlowShader2D
import noobroutes.utils.render.Color
import noobroutes.utils.render.getTextHeight
import noobroutes.utils.render.getTextWidth
import noobroutes.utils.render.text

class SwitchElement(
    val name: String,
    val scale: Float,
    initialValue: Boolean,
    x: Float,
    y: Float,
    w: Float,
    h: Float,

) : UiElement<Boolean>(x, y, w, h) {
    override var elementValue: Boolean = initialValue

    private val colorAnimation = ColorAnimation(150)
    private val linearAnimation = LinearAnimation<Float>(200)


    private inline val isHovered get() = MouseUtils.isAreaHovered(
        x + TEXT_OFFSET,
        y + halfHeight + getTextHeight(name, scale, ColorPalette.font),
        getTextWidth(name, scale, ColorPalette.font),
        getTextHeight(name, scale, ColorPalette.font),
    )




    override fun draw() {
        val hovered = isHovered

        val color = colorAnimation.get(
            ColorPalette.primary,
            ColorPalette.text,
            elementValue
        ).darkerIf(hovered, 0.7f)

        text(name, x + TEXT_OFFSET, y + h * 0.5f, color, 16f, fontType = ColorPalette.font, type = Font.BOLD)

    }

    override fun mouseLeftClicked() {
        if (!isHovered) return
        if (colorAnimation.start()) {
            linearAnimation.start()
            setValue(!elementValue)
        }
    }





}