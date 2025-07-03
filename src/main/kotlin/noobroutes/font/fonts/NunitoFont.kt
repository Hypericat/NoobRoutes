package noobroutes.font.fonts

import gg.essential.elementa.font.FontRenderer
import gg.essential.universal.UMatrixStack
import noobroutes.font.EssentialFont
import noobroutes.font.Font
import noobroutes.utils.render.Color
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.TextPos
import kotlin.math.max

object NunitoFont : Font {
    private lateinit var fontRenderer: FontRenderer

    override fun init() {
        fontRenderer = FontRenderer(
            EssentialFont.fromResource("/assets/fonts/nunito/Regular"),
            EssentialFont.fromResource("/assets/fonts/nunito/SemiBold")
        )
    }

    override fun text(text: String, x: Float, y: Float, color: Color, scale: Float, align: TextAlign, verticalAlign: TextPos, shadow: Boolean, type: Int) {
        if (color.isTransparent) return
        val drawX = when (align) {
            TextAlign.Left -> x
            TextAlign.Right -> x - getTextWidth(text, scale)
            TextAlign.Middle -> x - getTextWidth(text, scale) * 0.5f
        }

        val drawY = when (verticalAlign) {
            TextPos.Top -> y
            TextPos.Middle -> y - getTextHeight(text, scale) * 0.5f
            TextPos.Bottom -> y - getTextHeight(text, scale)
        }

        val typeText = if (type == Font.BOLD) "§l$text" else text

        fontRenderer.drawString(UMatrixStack.Compat.get(), typeText, color.javaColor, drawX, drawY, 1f, scale, shadow)
    }

    override fun getTextWidth(text: String, size: Float): Float {
        return fontRenderer.getStringWidth(text, size)
    }

    override fun getTextHeight(text: String, size: Float): Float {
        return fontRenderer.getStringHeight(text, size)
    }

    override fun wrappedText(text: String, x: Float, y: Float, w: Float, color: Color, size: Float, type: Int, shadow: Boolean) {
        if (color.isTransparent) return

        val words = text.split(" ")
        var line = ""
        var currentHeight = y + 2

        for (word in words) {
            if (getTextWidth(line + word, size) > w) {
                text(line, x, currentHeight, color, size, type = type, shadow = shadow)
                line = "$word "
                currentHeight += getTextHeight(line, size + 7)
            }
            else line += "$word "

        }
        text(line, x, currentHeight , color, size, type = type, shadow = shadow)
    }

    override fun wrappedTextBounds(text: String, width: Float, size: Float): Pair<Float, Float> {
        val words = text.split(" ")
        var line = ""
        var lines = 1
        var maxWidth = 0f

        for (word in words) {
            if (getTextWidth(line + word, size) > width) {
                maxWidth = max(maxWidth, getTextWidth(line, size))
                line = "$word "
                lines++
            }
            else line += "$word "

        }
        maxWidth = max(maxWidth, getTextWidth(line, size))

        return Pair(maxWidth, lines * getTextHeight(line, size + 3))
    }
}