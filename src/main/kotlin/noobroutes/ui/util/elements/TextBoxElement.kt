package noobroutes.ui.util.elements

import noobroutes.Core.logger
import noobroutes.ui.ColorPalette
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.MouseUtils.mouseX
import noobroutes.ui.util.UiElement
import noobroutes.utils.render.*


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
    data class CharHitbox(val left: Float, val right: Float)

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
        ) {
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
            return
        }
    }

    fun getTextOrigin(): Pair<Float, Float> {
        val width = stringWidth(elementValue, textScale, minWidth, textPadding)
        val textWidth = getTextWidth(elementValue, textScale)
        when (boxType) {
            TextBoxType.NORMAL -> {
                return 0f to 0f
            }
            TextBoxType.GAP -> {
                val textOrigin = when (textAlign) {
                    TextAlign.Right -> width - textPadding - textWidth
                    TextAlign.Middle -> width * 0.5f - textWidth * 0.5f
                    TextAlign.Left -> textPadding
                }
                return (x + textOrigin) to (y + h * 0.5f)
            }
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        return super.keyTyped(typedChar, keyCode)
    }

    var lastClickedXPosition: Float = 0f


    var listeningTextSelection = false
    val charHitboxes = mutableListOf<CharHitbox>()
    var insertionCursor: Int = 0
    var selectionStart = 0
    var selectionEnd = 0
    val hasSelection: Boolean get() = selectionStart != selectionEnd


    //(x + textOrigin) to (y + h * 0.5f)
    override fun draw() {
        drawTextBoxWithGapTitle(elementValue, name, x, y, minWidth, h, radius, textScale, textPadding, boxColor, textAlign)
        val (textX, textY) = getTextOrigin()
        if (listeningTextSelection) {
            selectionEnd = getCursorIndexFromX(mouseX - xOrigin - textX)
            logger.info("selectionStart: $selectionStart, selectionEnd: $selectionEnd")
        }

        if (!hasSelection) return
        val start = minOf(selectionStart, selectionEnd)
        val end = maxOf(selectionStart, selectionEnd)
        if (start in 0 until end && end <= charHitboxes.size) {
            val selectionStartX = textX + charHitboxes[start].left
            val selectedText = elementValue.substring(start, end)
            val selectionWidth = getTextWidth(selectedText, textScale)
            val textHeight = getTextHeight(selectedText, textScale)
            roundedRectangle(selectionStartX, textY - textHeight * 0.5f, selectionWidth, textHeight, Color.BLUE, 0f)
        }

    }

    var listening = false
    private inline val isHovered get() = MouseUtils.isAreaHovered(xOrigin + x, yOrigin + y, stringWidth(elementValue, textScale, minWidth, textPadding), h)

    fun getCursorIndexFromX(mouseX: Float): Int {
        for ((i, hitbox) in charHitboxes.withIndex()) {
            val mid = (hitbox.left + hitbox.right) * 0.5f
            if (mouseX < mid) return i
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
        elementValue.forEach { ch ->
            val w = getTextWidth(ch.toString(), textScale)
            charHitboxes += CharHitbox(currentX, currentX + w)
            currentX += w
        }
    }


    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton != 0) return false
        if (!isHovered) return false
        val (textX, textY) = getTextOrigin()
        lastClickedXPosition = mouseX
        generateCharacterHitboxes()
        selectionStart = getCursorIndexFromX(mouseX - xOrigin - textX)
        selectionEnd = selectionStart
        insertionCursor = selectionStart
        listeningTextSelection = true
        return true

    }

    override fun mouseReleased(): Boolean {
        listeningTextSelection = false
        return false
    }




}