package me.odinmain.font

import gg.essential.elementa.font.FontRenderer
import gg.essential.elementa.font.data.Font
import gg.essential.universal.UMatrixStack
import me.odinmain.OdinMain.mc
import me.odinmain.utils.noControlCodes
import me.odinmain.utils.render.*
import net.minecraft.client.renderer.GlStateManager
import kotlin.math.max

object OdinFont {

    private lateinit var fontRenderer: net.minecraft.client.gui.FontRenderer

    const val REGULAR = 1
    const val BOLD = 2

    fun init() {
        fontRenderer = mc.fontRendererObj
    }

    fun text(text: String, x: Float, y: Float, color: Color, scale: Float, align: TextAlign = TextAlign.Left, verticalAlign: TextPos = TextPos.Middle, shadow: Boolean = false, type: Int = REGULAR) {
        if (color.isTransparent) return
        val reducedScale = scale/8
        val drawX = when (align) {
            TextAlign.Left   -> x
            TextAlign.Right  -> x - getTextWidth(text, reducedScale)
            TextAlign.Middle -> x - getTextWidth(text, reducedScale) / 2f
        }

        val drawY = when (verticalAlign) {
            TextPos.Top    -> y
            TextPos.Middle -> y - getTextHeight(reducedScale) / 2f
            TextPos.Bottom -> y - getTextHeight(reducedScale)
        }
        GlStateManager.pushMatrix()
        GlStateManager.translate(drawX.toDouble(), drawY.toDouble(), 0.0)
        GlStateManager.scale(reducedScale, reducedScale, 1.0f)

        val typeText = if (type == BOLD) "§l$text" else text
        fontRenderer.drawString(typeText, 0, 0, color.rgba)
        GlStateManager.popMatrix()
    }

    fun getTextWidth(text: String, size: Float): Int {
        return (fontRenderer.getStringWidth(text.noControlCodes) * size).toInt()
    }

    val fontHeight: Int
        get() = fontRenderer.FONT_HEIGHT

    fun getTextHeight(size: Float): Int {
        return (fontHeight * size).toInt()
    }

    fun wrappedText(text: String, x: Float, y: Float, w: Float, color: Color, size: Float, type: Int = REGULAR, shadow: Boolean = false) {
        if (color.isTransparent) return
        val words = text.split(" ")
        var line = ""
        var currentHeight = y + 2

        for (word in words) {
            if (getTextWidth(line + word, size / 8) > w) {
                text(line, x, currentHeight, color, size, type = type, shadow = shadow)
                line = "$word "
                currentHeight += getTextHeight((size + 7) / 8)
            }
            else line += "$word "
        }
        text(line, x, currentHeight , color, size, type = type, shadow = shadow)
    }

    fun wrappedTextBounds(text: String, width: Float, size: Float): Pair<Float, Float> {
        val reducedScale = size/8
        val words = text.split(" ")
        var line = ""
        var lines = 1
        var maxWidth = 0f

        for (word in words) {
            if (getTextWidth(line + word, reducedScale) > width) {
                maxWidth = max(maxWidth, getTextWidth(line, reducedScale).toFloat())
                line = "$word "
                lines++
            }
            else line += "$word "

        }
        maxWidth = max(maxWidth, getTextWidth(line, reducedScale).toFloat())

        return Pair(maxWidth, lines * getTextHeight(reducedScale + 3).toFloat())
    }

}