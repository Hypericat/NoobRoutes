package noobroutes.font.fonts

import net.minecraft.client.gui.FontRenderer
import net.minecraft.client.renderer.GlStateManager
import noobroutes.Core.mc
import noobroutes.font.Font
import noobroutes.utils.noControlCodes
import noobroutes.utils.render.Color
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.TextPos
import kotlin.math.max

object MinecraftFont : Font {

    private lateinit var fontRenderer: FontRenderer


    override fun init() {
        fontRenderer = mc.fontRendererObj
    }

    override fun xOrigin(text: String, x: Float, align: TextAlign, scale: Float): Float {
        val reducedScale = scale * 0.125f
        return when (align) {
            TextAlign.Left -> x
            TextAlign.Right -> x - OdinFont.getTextWidth(text, reducedScale)
            TextAlign.Middle -> x - OdinFont.getTextWidth(text, reducedScale) * 0.5f
        }
    }

    override fun text(text: String, x: Float, y: Float, color: Color, scale: Float, align: TextAlign, verticalAlign: TextPos, shadow: Boolean, type: Int) {
        if (color.isTransparent) return
        val reducedScale = scale * 0.125f
        val drawX = when (align) {
            TextAlign.Left   -> x
            TextAlign.Right  -> x - getTextWidth(text, reducedScale)
            TextAlign.Middle -> x - getTextWidth(text, reducedScale) * 0.5f
        }

        val drawY = when (verticalAlign) {
            TextPos.Top    -> y
            TextPos.Middle -> y - getTextHeight(text, reducedScale) * 0.5f
            TextPos.Bottom -> y - getTextHeight(text,reducedScale)
        }
        GlStateManager.pushMatrix()
        GlStateManager.translate(drawX.toDouble(), drawY.toDouble(), 0.0)
        GlStateManager.scale(reducedScale, reducedScale, 1.0f)

        val typeText = if (type == Font.BOLD) "§l$text" else text
        fontRenderer.drawString(typeText, 0, 0, color.rgba)
        GlStateManager.popMatrix()
    }

    override fun getTextWidth(text: String, size: Float): Float {
        return fontRenderer.getStringWidth(text.noControlCodes) * size// * 0.125f
    }

    private val fontHeight: Int
        get() = fontRenderer.FONT_HEIGHT

    override fun getTextHeight(text: String, size: Float): Float {
        return fontHeight * size// * 0.125f
    }

    override fun wrappedText(text: String, x: Float, y: Float, w: Float, color: Color, size: Float, type: Int, shadow: Boolean) {
        if (color.isTransparent) return
        val words = text.split(" ")
        var line = ""
        var currentHeight = y + 2

        for (word in words) {
            if (getTextWidth(line + word, size * 0.125f) > w) {
                text(line, x, currentHeight, color, size, type = type, shadow = shadow)
                line = "$word "
                currentHeight += getTextHeight("", (size + 7) * 0.125f).toInt()
            }
            else line += "$word "
        }
        text(line, x, currentHeight , color, size, type = type, shadow = shadow)
    }

    override fun wrappedTextBounds(text: String, width: Float, size: Float): Pair<Float, Float> {
        val reducedScale = size * 0.125f
        val words = text.split(" ")
        var line = ""
        var lines = 1
        var maxWidth = 0f

        for (word in words) {
            if (getTextWidth(line + word, reducedScale) > width) {
                maxWidth = max(maxWidth, getTextWidth(line, reducedScale))
                line = "$word "
                lines++
            }
            else line += "$word "

        }
        maxWidth = max(maxWidth, getTextWidth(line, reducedScale))

        return Pair(maxWidth, lines * getTextHeight("", reducedScale + 3))
    }

}