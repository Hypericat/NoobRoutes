package noobroutes.font


import noobroutes.utils.render.Color
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.TextPos

//C:\Users\wadey\Downloads\msdf-atlas-gen.exe -font "Downloads\Lexend\Lexend-Regular.ttf" -type msdf -imageout "Downloads\Regular.png" -json "Downloads\Regular.json" -format png -dimensions 512 512 -pxrange 16 -emrange 1 -yorigin bottom -size 32
typealias EssentialFont = gg.essential.elementa.font.data.Font

interface Font {
    fun text(text: String, x: Float, y: Float, color: Color, scale: Float, align: TextAlign = TextAlign.Left, verticalAlign: TextPos = TextPos.Middle, shadow: Boolean = false, type: Int = REGULAR)
    fun getTextWidth(text: String, size: Float): Float
    fun getTextHeight(text: String = "", size: Float): Float
    fun wrappedText(text: String, x: Float, y: Float, w: Float, color: Color, size: Float, type: Int = REGULAR, shadow: Boolean = false)
    fun wrappedTextBounds(text: String, width: Float, size: Float): Pair<Float, Float>
    fun init()
    companion object {
        const val REGULAR = 1
        const val BOLD = 2

    }
}