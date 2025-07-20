package noobroutes.ui.util.elements

import noobroutes.Core.logger
import noobroutes.ui.ColorPalette
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.UiElement
import noobroutes.utils.render.Color
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.getTextHeight
import noobroutes.utils.render.getTextWidth
import noobroutes.utils.render.popStencil
import noobroutes.utils.render.rectangleOutline
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.stencilRoundedRectangle
import noobroutes.utils.render.text
import kotlin.math.round


//implement copy and paste
//make it so you can add custom string filters
class TextBoxElement(
    val name: String,
    x: Float,
    y: Float,
    var minWidth: Float,
    var h: Float,
    val textScale: Float,
    val textAlign: TextAlign,
    val radius: Float,
    val textPadding: Float,
    val boxColor: Color,
    var maxCharacters: Int,
    val boxType: TextBoxType,
    override var elementValue: String
) : UiElement(x, y), ElementValue<String> {
    enum class TextBoxType{
        GAP,
        NORMAL
    }
    data class CharHitbox(val char: Char, val left: Float, val right: Float)

    override val elementValueChangeListeners = mutableListOf<(String) -> Unit>()



    companion object {
        const val TEXT_BOX_THICKNESS = 3f
        const val TEXT_BOX_GAP_TEXT_MULTIPLIER = 0.1f

        private fun stringWidth(text: String, scale: Float, min: Float, textPadding: Float): Float {
            return getTextWidth(text, scale).coerceAtLeast(min) + textPadding * 2
        }

        fun drawTextBoxWithGapTitle(
            text: String,
            name: String,
            x: Float,
            y: Float,
            minWidth: Float,
            height: Float,
            radius: Float,
            textScale: Float = 12f,
            textPadding: Float = 6f,
            boxColor: Color,
            textAlign: TextAlign = TextAlign.Left
        ): Pair<Float, Float> {
            val width = stringWidth(text, textScale, minWidth, textPadding)
            val nameWidth = getTextWidth(name, textScale) + textPadding
            val nameOrigin = when (textAlign) {
                TextAlign.Right -> width * 0.75f
                TextAlign.Middle -> width * 0.5f
                TextAlign.Left -> width * 0.25f
            }
            stencilRoundedRectangle(
                x + nameOrigin - nameWidth * 0.5f,
                y,
                nameWidth,
                TEXT_BOX_THICKNESS,
                0f,
                0.5f,
                true
            )
            rectangleOutline(
                x,
                y,
                width,
                height,
                boxColor,
                radius,
                TEXT_BOX_THICKNESS
            )
            popStencil()

            val textOrigin = when (textAlign) {
                TextAlign.Right -> width - textPadding
                TextAlign.Middle -> width * 0.5f
                TextAlign.Left -> textPadding
            }
            text(
                name,
                x + nameOrigin,
                y + getTextHeight(name, textScale) * TEXT_BOX_GAP_TEXT_MULTIPLIER,
                ColorPalette.text,
                textScale,
                align = TextAlign.Middle
            )
            text(text, x + textOrigin, y + height * 0.5f, ColorPalette.text, textScale, align = textAlign)
            return Pair(x + textOrigin, y + height * 0.5f)
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        return super.keyTyped(typedChar, keyCode)
    }

    var lastClickedXPosition: Float = 0f

    var deltaMouseX: Float = 0f


    var listeningTextSelection = false
    var highlightedString = ""
    val charHitboxes = mutableListOf<CharHitbox>()
    var mouseHighlightOrigin: Int = 0



    var selectionStart = 0
    var selectionEnd = 0
    val hasSelection: Boolean get() = selectionStart != selectionEnd

    override fun draw() {
        roundedRectangle(x, y, stringWidth(elementValue, textScale, minWidth, textPadding), h, Color.GREEN)
        if (listeningTextSelection) {
            val localX = MouseUtils.mouseX - x
            val currentIndex = getCursorIndexFromX(localX)
            selectionEnd = currentIndex
        }
        val textOrigin = drawTextBoxWithGapTitle(elementValue, name, x, y, minWidth, h, radius, textScale, textPadding, boxColor, textAlign)

        if (!hasSelection) return
        val start = minOf(selectionStart, selectionStart)
        val end = maxOf(selectionStart, selectionEnd)
        if (start in 0 until end && end <= charHitboxes.size) {
            val xStart = charHitboxes[start].left + textOrigin.first
            val height = getTextHeight(highlightedString, textScale)
            highlightedString = elementValue.substring(start, end)
            roundedRectangle(
                xStart,
                y - height * 0.5f,
                getTextWidth(highlightedString, textScale),
                height,
                Color.BLUE,
                0f
            )
        }

    }

    var listening = false
    private inline val isHovered get() = MouseUtils.isAreaHovered(x, y, stringWidth(elementValue, textScale, minWidth, textPadding), h)

    fun getCursorIndexFromX(localX: Float): Int {
        for ((i, hitbox) in charHitboxes.withIndex()) {
            val mid = (hitbox.left + hitbox.right) * 0.5f
            if (localX < mid) return i
        }
        return elementValue.length
    }


    //make sure that mouseX actually matches the rendering positions of the text. (scaling or something idk)
    private fun generateCharacterHitboxes(){
        charHitboxes.clear()
        val width = stringWidth(elementValue, textScale, minWidth, textPadding)
        val textWidth = getTextWidth(elementValue, textScale)
        var currentX = when (textAlign) {
            TextAlign.Right -> width - textPadding - textWidth
            TextAlign.Middle -> width * 0.5f - textWidth * 0.5f
            TextAlign.Left -> textPadding
        }
        elementValue.forEachIndexed { index, char ->
            val width = getTextWidth(char.toString(), textScale)
            val startX = currentX
            currentX += width
            charHitboxes.add(CharHitbox(char, startX, currentX))
        }
    }


    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton != 0) return false
        if (!isHovered) return false
        lastClickedXPosition = MouseUtils.mouseX
        val localX = MouseUtils.mouseX - x
        selectionStart = getCursorIndexFromX(localX)
        selectionEnd = selectionStart
        mouseHighlightOrigin = selectionStart
        listeningTextSelection = true
        generateCharacterHitboxes()

        return true

    }

    override fun mouseReleased(): Boolean {
        listeningTextSelection = false
        return false
    }




}