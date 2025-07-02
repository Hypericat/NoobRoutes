package noobroutes.font

import noobroutes.font.fonts.MinecraftFont.REGULAR
import noobroutes.utils.render.Color
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.TextPos

typealias EssentialFont = gg.essential.elementa.font.data.Font

interface Font {
    fun text(text: String, x: Float, y: Float, color: Color, scale: Float, align: TextAlign = TextAlign.Left, verticalAlign: TextPos = TextPos.Middle, shadow: Boolean = false, type: Int = REGULAR)
    fun getTextWidth(text: String, size: Float): Float
    fun getTextHeight(text: String = "", size: Float): Float
    fun wrappedText(text: String, x: Float, y: Float, w: Float, color: Color, size: Float, type: Int = REGULAR, shadow: Boolean = false)
    fun wrappedTextBounds(text: String, width: Float, size: Float): Pair<Float, Float>
    fun init()


}