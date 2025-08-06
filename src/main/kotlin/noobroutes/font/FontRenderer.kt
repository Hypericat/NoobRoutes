package noobroutes.font

import gg.essential.elementa.font.FontRenderer
import gg.essential.universal.UMatrixStack
import noobroutes.font.EssentialFont
import noobroutes.ui.ColorPalette
import noobroutes.utils.render.Color
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.TextPos
import kotlin.math.max


typealias EssentialFont = gg.essential.elementa.font.data.Font
@Suppress("Unused")
object FontRenderer {
    private val fonts = mutableMapOf<FontType, FontRenderer>()
    private inline val fontRenderer: FontRenderer get() = fonts[ColorPalette.font]!!

    const val REGULAR = 1
    const val BOLD = 2


    init {
        fonts[FontType.MANROPE] = FontRenderer(
            EssentialFont.fromResource("/assets/fonts/manrope/Regular"),
            EssentialFont.fromResource("/assets/fonts/manrope/SemiBold")
        )
        fonts[FontType.NUNITO] = FontRenderer(
            EssentialFont.fromResource("/assets/fonts/nunito/Regular"),
            EssentialFont.fromResource("/assets/fonts/nunito/Bold")
        )
        fonts[FontType.ODIN] = FontRenderer(
            EssentialFont.fromResource("/assets/fonts/odin/Regular"),
            EssentialFont.fromResource("/assets/fonts/odin/SemiBold")
        )
        fonts[FontType.LEXEND] = FontRenderer(
            EssentialFont.fromResource("/assets/fonts/lexend/Regular"),
            EssentialFont.fromResource("/assets/fonts/lexend/SemiBold")
        )
        fonts[FontType.MANUFACTURING_CONSENT] = FontRenderer(
            EssentialFont.fromResource("/assets/fonts/manufacturingconsent/Regular"),
            EssentialFont.fromResource("/assets/fonts/manufacturingconsent/SemiBold")
        )
    }

    fun xOrigin(text: String, x: Float, align: TextAlign, scale: Float): Float {
        return when (align) {
            TextAlign.Left -> x
            TextAlign.Right -> x - getTextWidth(text, scale)
            TextAlign.Middle -> x - getTextWidth(text, scale) * 0.5f
        }
    }

    fun text(text: String, x: Float, y: Float, color: Color, scale: Float, align: TextAlign = TextAlign.Left, verticalAlign: TextPos = TextPos.Middle, shadow: Boolean = false, type: Int = REGULAR) {
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

        val typeText = if (type == BOLD) "§l$text" else text

        fontRenderer.drawString(UMatrixStack.Compat.get(), typeText, color.javaColor, drawX, drawY, 1f, scale, shadow)
    }

    fun getTextWidth(text: String, size: Float): Float {
        return fontRenderer.getStringWidth(text, size)
    }

    fun getTextHeight(text: String = "M", size: Float): Float {
        return fontRenderer.getStringHeight(text, size)
    }

    fun wrappedText(text: String, x: Float, y: Float, w: Float, color: Color, size: Float, type: Int = REGULAR, shadow: Boolean = false) {
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

    fun wrappedTextBounds(text: String, width: Float, size: Float): Pair<Float, Float> {
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