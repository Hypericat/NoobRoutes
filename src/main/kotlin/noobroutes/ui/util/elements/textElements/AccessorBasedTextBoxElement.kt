package noobroutes.ui.util.elements.textElements

import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.ColorPalette
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.ui.util.shader.GapOutlineShader
import noobroutes.utils.clock.Clock
import noobroutes.utils.render.Color
import noobroutes.utils.render.ColorUtil.withAlpha
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.TextPos
import noobroutes.utils.render.gapOutline
import noobroutes.utils.render.getTextHeight
import noobroutes.utils.render.getTextWidth
import noobroutes.utils.render.popStencil
import noobroutes.utils.render.rectangleOutline
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.stencilRoundedRectangle
import noobroutes.utils.render.text
import noobroutes.utils.writeToClipboard
import org.lwjgl.input.Keyboard
import kotlin.math.max
import kotlin.math.min

//implement copy and paste
//make it so you can add custom string filters
class AccessorBasedTextBoxElement(
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
    val boxType: TextBoxElement.TextBoxType,
    val boxThickness: Float = 3f,
    val keyWhiteList: List<Int>,
    val placeHolder: String = "",
    val verticalAlign: TextPos = TextPos.Middle,
    val getter: () -> String,
    val setter: (String) -> Unit
) : UiElement(x, y){

    constructor(
        name: String,
        x: Float,
        y: Float,
        minWidth: Float,
        h: Float,
        textScale: Float,
        textAlign: TextAlign,
        radius: Float,
        textPadding: Float,
        boxColor: Color,
        maxCharacters: Int,
        boxType: TextBoxElement.TextBoxType,
        boxThickness: Float = 3f,
        placeHolder: String = "",
        verticalAlign: TextPos = TextPos.Middle,
        getter: () -> String,
        setter: (String) -> Unit
    ) : this(name, x, y, minWidth, h, textScale, textAlign, radius, textPadding, boxColor, maxCharacters, boxType, boxThickness, defaultWhiteList, placeHolder, verticalAlign, getter, setter)

    inline var elementValue
        get() = getter.invoke()
        set(value) {setter(value)}

    companion object {
        const val TEXT_BOX_GAP_TEXT_MULTIPLIER = 0.1f

        private fun stringWidth(text: String, scale: Float, min: Float, textPadding: Float): Float {
            return getTextWidth(text, scale).coerceAtLeast(min) + textPadding * 2
        }

        private var activeTextBoxElement: AccessorBasedTextBoxElement? = null

        val defaultWhiteList = listOf(
            Keyboard.KEY_A,
            Keyboard.KEY_B,
            Keyboard.KEY_C,
            Keyboard.KEY_D,
            Keyboard.KEY_E,
            Keyboard.KEY_F,
            Keyboard.KEY_G,
            Keyboard.KEY_H,
            Keyboard.KEY_I,
            Keyboard.KEY_J,
            Keyboard.KEY_K,
            Keyboard.KEY_L,
            Keyboard.KEY_M,
            Keyboard.KEY_N,
            Keyboard.KEY_O,
            Keyboard.KEY_P,
            Keyboard.KEY_Q,
            Keyboard.KEY_R,
            Keyboard.KEY_S,
            Keyboard.KEY_T,
            Keyboard.KEY_U,
            Keyboard.KEY_V,
            Keyboard.KEY_W,
            Keyboard.KEY_X,
            Keyboard.KEY_Y,
            Keyboard.KEY_Z,
            Keyboard.KEY_0,
            Keyboard.KEY_1,
            Keyboard.KEY_2,
            Keyboard.KEY_3,
            Keyboard.KEY_4,
            Keyboard.KEY_5,
            Keyboard.KEY_6,
            Keyboard.KEY_7,
            Keyboard.KEY_8,
            Keyboard.KEY_9,
            Keyboard.KEY_SECTION,
            Keyboard.KEY_COLON,
            Keyboard.KEY_SEMICOLON,
            Keyboard.KEY_SLASH,
            Keyboard.KEY_SPACE
        )
        val textSelectionColor = Color(76, 204, 252, 0.651f)
    }
    private inline val fontHeight get() = getTextHeight("M", textScale)

    private val copyColorAnimation = ColorAnimation(500)

    private inline val controlHeld get() = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL)
    private inline val shiftHeld get() = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT)
    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (!listening) return false
        resetCursorBlink()
        when (keyCode) {
            Keyboard.KEY_ESCAPE, Keyboard.KEY_NUMPADENTER, Keyboard.KEY_RETURN -> {
                resetSelection()

                listening = false
            }
            Keyboard.KEY_BACK -> {
                if (elementValue.isEmpty()) return true
                if (hasSelection) {
                    removeSelectionFromString()
                    return true
                }

                if (controlHeld) {
                    val index = getWordIndexLeft()
                    elementValue = elementValue.removeRange(index, insertionCursor)
                    insertionCursor = index
                    return true
                }
                if (insertionCursor > 0) {
                    insertionCursor--
                    elementValue = elementValue.removeRange(insertionCursor, insertionCursor + 1)
                }
            }
            Keyboard.KEY_C -> {
                if (controlHeld) {
                    if (hasSelection) {
                        copyColorAnimation.start(true)
                        writeToClipboard(elementValue.substring(minSelection, maxSelection), "")
                    }
                    return true
                }
                if (keyCode in keyWhiteList) typeCharacter(typedChar)
            }
            Keyboard.KEY_X -> {
                if (controlHeld) {
                    if (hasSelection) {
                        writeToClipboard(elementValue.substring(minSelection, maxSelection), "")
                        removeSelectionFromString()
                    }
                    return true
                }
                if (keyCode in keyWhiteList) typeCharacter(typedChar)
            }
            Keyboard.KEY_V -> {
                if (controlHeld) {
                    GuiScreen.getClipboardString()?.let {
                        var filteredText = it.filter { char ->
                            Keyboard.getKeyIndex(char.uppercase()) in keyWhiteList || (char == ' ' && keyWhiteList.contains(
                                Keyboard.KEY_SPACE))
                        }
                        if (hasSelection) {
                            removeSelectionFromString()
                        }
                        filteredText = filteredText.take((maxCharacters - elementValue.length).coerceAtLeast(0))
                        elementValue = elementValue.substring(0, insertionCursor) + filteredText + elementValue.substring(insertionCursor)
                        generateCharacterHitboxes()
                        insertionCursor += filteredText.length
                    }
                    return true
                }
                if (keyCode in keyWhiteList) typeCharacter(typedChar)
            }
            Keyboard.KEY_A -> {
                if (controlHeld) {
                    selectionStart = 0
                    selectionEnd = elementValue.length
                    return true
                }
                if (keyCode in keyWhiteList) typeCharacter(typedChar)
            }
            in keyWhiteList -> {
                typeCharacter(typedChar)
            }
            Keyboard.KEY_RIGHT -> {
                val shiftAmount = if (controlHeld) (getWordIndexRight() - insertionCursor) else 1
                if (shiftHeld) {
                    if (hasSelection) {
                        if (selectionEnd == elementValue.length) return true
                        selectionEnd += shiftAmount
                        insertionCursor += shiftAmount
                        return true
                    }
                    if (insertionCursor == elementValue.length) return true
                    selectionStart = insertionCursor
                    insertionCursor += shiftAmount
                    selectionEnd = insertionCursor
                    return true
                }

                if (hasSelection) {
                    insertionCursor = maxSelection
                    resetSelection()
                    return true
                }
                shiftRight(shiftAmount)
            }
            Keyboard.KEY_LEFT -> {
                val shiftAmount = if (controlHeld) insertionCursor - getWordIndexLeft() else 1
                if (shiftHeld) {
                    if (hasSelection) {
                        if (selectionEnd == 0) return true
                        selectionEnd -= shiftAmount
                        insertionCursor -= shiftAmount
                        return true
                    }

                    if (insertionCursor == 0) return true
                    selectionStart = insertionCursor
                    insertionCursor -= shiftAmount
                    selectionEnd = insertionCursor
                    return true
                }

                if (hasSelection) {
                    insertionCursor = minSelection
                    resetSelection()
                    return true
                }
                shiftLeft(shiftAmount)
            }
        }
        return true
    }

    private fun typeCharacter(typedChar: Char){
        if (hasSelection) {
            removeSelectionFromString()
        }
        if (elementValue.length >= maxCharacters) return
        elementValue = elementValue.substring(0, insertionCursor) + typedChar.toString() + elementValue.substring(insertionCursor)
        insertionCursor++
        generateCharacterHitboxes()
    }

    private fun removeSelectionFromString(){
        val end = maxSelection
        val start = minSelection
        resetSelection()
        insertionCursor = start
        elementValue = elementValue.removeRange(start, end)
    }

    private fun shiftRight(amount: Int = 1) {
        if (insertionCursor + amount > elementValue.length) insertionCursor = elementValue.length
        else insertionCursor += amount
    }

    private fun shiftLeft(amount: Int = 1) {
        if (insertionCursor - amount < 0) insertionCursor = 0
        else insertionCursor -= amount
    }

    fun getWordIndexLeft(): Int {
        var index = 0
        for (i in 0 until insertionCursor) {
            if (elementValue[i] == ' ') index = i
        }
        return index
    }

    fun getWordIndexRight(): Int {
        var index = elementValue.length
        for (i in insertionCursor until elementValue.length) {
            if (elementValue[i] == ' ') {
                index = i + 1
                break
            }
        }
        return index
    }

    private inline val minSelection get() = min(selectionStart, selectionEnd)
    private inline val maxSelection get() = max(selectionStart, selectionEnd)
    val cursorClock = Clock(1000)
    var lastClickedXPosition: Float = 0f
    var listeningTextSelection = false
    val charHitboxes = mutableListOf<Float>()
    var insertionCursor: Int = 0
    var selectionStart = 0
    var selectionEnd = 0
    val hasSelection: Boolean get() = selectionStart != selectionEnd


    override fun draw() {
        when (boxType) {
            TextBoxElement.TextBoxType.NORMAL -> drawTextBox()
            TextBoxElement.TextBoxType.GAP -> drawTextBoxWithGapTitle()
            TextBoxElement.TextBoxType.NO_BOX -> drawTextBoxNoBox()
        }
        if (!listening) return

        val (textX, textY) = getTextOrigin()
        if (listeningTextSelection) {
            val cursorIndex = getCursorIndexFromX(MouseUtils.mouseX - (xOrigin + textX) * getEffectiveXScale())
            if (cursorIndex != selectionEnd) resetCursorBlink()
            selectionEnd = cursorIndex
            insertionCursor = selectionEnd
        }
        if (hasSelection) drawSelection(textX, textY, fontHeight)
        drawCursor(textX)
    }

    var listening = false
    private inline val isHovered get() = isAreaHovered(x, y, stringWidth(elementValue, textScale, minWidth, textPadding), h)

    fun getCursorIndexFromX(mouseX: Float): Int {
        for ((i, hitbox) in charHitboxes.withIndex()) {
            if (mouseX < hitbox) return i
        }
        return elementValue.length
    }

    private fun generateCharacterHitboxes(){
        charHitboxes.clear()
        var currentX = when (textAlign) {
            TextAlign.Right -> -textPadding
            TextAlign.Middle -> 0f
            TextAlign.Left -> textPadding
        }
        elementValue.forEach { ch ->
            val w = getTextWidth(ch.toString(), textScale) * getEffectiveXScale()
            charHitboxes += currentX + w * 0.5f
            currentX += w
        }
    }


    val resetClickStageClock = Clock(250)
    var clickSelectStage = 0
    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton != 0) return false
        if (!isHovered) {
            listening = false
            resetSelection()
            return false
        }
        if (resetClickStageClock.hasTimePassed(false)) clickSelectStage = 0
        resetCursorBlink()
        activeTextBoxElement?.let {
            it.listening = false
            it.resetSelection()
        }
        activeTextBoxElement = this
        listening = true
        val (textX, textY) = getTextOrigin()
        lastClickedXPosition = MouseUtils.mouseX
        generateCharacterHitboxes()
        selectionStart = getCursorIndexFromX(MouseUtils.mouseX - (xOrigin + textX) * getEffectiveXScale())
        selectionEnd = selectionStart
        insertionCursor = selectionStart
        listeningTextSelection = true
        when (clickSelectStage) {
            0 -> {
                clickSelectStage++
                resetClickStageClock.update()
            }
            1 -> {
                resetSelection()
                selectionStart = getWordIndexLeft()
                if (selectionStart != 0) selectionStart++
                selectionEnd = getWordIndexRight()
                clickSelectStage++
                resetClickStageClock.update()
            }
            2 -> {
                resetSelection()
                selectionStart = 0
                selectionEnd = elementValue.length
                clickSelectStage = 0
            }
        }
        return true
    }

    override fun mouseReleased(): Boolean {
        listeningTextSelection = false
        return false
    }

    private fun drawSelection(textX: Float, textY: Float, textHeight: Float){
        val end = maxSelection
        val start = minSelection

        if (start >= 0 && end <= elementValue.length && start < end) {
            val selectionStartX = textX + getTextWidth(elementValue.substring(0, start), textScale) + getPadding()
            val selectedText = elementValue.substring(start, end)
            val selectionWidth = getTextWidth(selectedText, textScale * 0.97f)

            roundedRectangle(
                selectionStartX,
                textY - textHeight * 0.5f,
                selectionWidth,
                textHeight,
                copyColorAnimation.get(ColorPalette.clickGUIColor.withAlpha(0.651f), textSelectionColor, false),
                0f
            )
        }
    }

    private fun getPadding(): Float {
        return when (textAlign) {
            TextAlign.Left -> textPadding
            TextAlign.Middle -> 0f
            TextAlign.Right -> -textPadding
        }
    }

    fun getTextOrigin(): Pair<Float, Float> {
        val width = stringWidth(elementValue, textScale, minWidth, textPadding)
        val textWidth = getTextWidth(elementValue, textScale)
        val boxOffset = when (textAlign) {
            TextAlign.Left -> 0f
            TextAlign.Middle -> -width * 0.5f
            TextAlign.Right -> -width
        }

        val textOrigin = when (textAlign) {
            TextAlign.Right -> width - textWidth
            TextAlign.Middle -> width * 0.5f - textWidth * 0.5f
            TextAlign.Left -> 0f
        }
        return (x + textOrigin + boxOffset) to (y + h * 0.5f)
    }

    private fun drawCursor(textX: Float){
        cursorClock.hasTimePassed(true)
        if (cursorClock.getTime() > 500L) return
        val textHeight = fontHeight
        val insertionCursorOrigin = getPadding()

        val insertionCursorX = if (elementValue.isEmpty()) insertionCursorOrigin else
            when (insertionCursor) {
            elementValue.length -> {
                getTextWidth(elementValue, textScale) + insertionCursorOrigin
            }
            0 -> {
                insertionCursorOrigin
            }
            else -> {
                insertionCursorOrigin + getTextWidth(
                    elementValue.substring(
                        0,
                        insertionCursor.coerceAtMost(elementValue.length - 1)
                    ), textScale
                )
            }
        }
        roundedRectangle(
            insertionCursorX + textX,
            y + h * 0.5f - textHeight * 0.5f,
            2f,
            textHeight,
            Color.Companion.WHITE
        )
    }
    private fun resetCursorBlink(){
        cursorClock.setTime(System.currentTimeMillis() + 500L)
    }

    private fun drawTextBoxWithGapTitle() {
        GlStateManager.pushMatrix()
        val displayString = elementValue.ifBlank { placeHolder }
        val width = stringWidth(displayString, textScale, minWidth, textPadding)
        val nameWidth = getTextWidth(name, textScale) + textPadding
        val nameOrigin = when (textAlign) {
            TextAlign.Right -> width * 0.75f
            TextAlign.Middle -> width * 0.5f
            TextAlign.Left -> width * 0.25f
        }

        val boxOffset = when (textAlign) {
            TextAlign.Left -> 0f
            TextAlign.Middle -> -width * 0.5f
            TextAlign.Right -> -width
        }


        translate(boxOffset, 0f)
        val textOrigin = when (textAlign) {
            TextAlign.Right -> width - textPadding
            TextAlign.Middle -> width * 0.5f
            TextAlign.Left -> textPadding
        }
        text(
            name,
            x + nameOrigin,
            y + fontHeight * TEXT_BOX_GAP_TEXT_MULTIPLIER,
            ColorPalette.textColor,
            textScale,
            align = TextAlign.Middle
        )
        text(
            displayString,
            x + textOrigin,
            y + h * 0.5f,
            ColorPalette.textColor,
            textScale,
            align = textAlign,
            verticalAlign = verticalAlign
        )


        gapOutline(
            x,
            y,
            width,
            h,
            radius,
            boxColor,
            boxThickness,
            nameOrigin,
            boxThickness * 0.5f,
            nameWidth,
            boxThickness
        )


        GlStateManager.popMatrix()
    }
    private fun drawTextBox(){
        GlStateManager.pushMatrix()
        val displayString = elementValue.ifBlank { placeHolder }
        val width = stringWidth(displayString, textScale, minWidth, textPadding)

        val boxOffset = when (textAlign) {
            TextAlign.Left -> 0f
            TextAlign.Middle -> -width * 0.5f
            TextAlign.Right -> -width
        }
        translate(boxOffset, 0f)

        rectangleOutline(x, y, width, h, boxColor, radius, boxThickness)

        val textOrigin = when (textAlign) {
            TextAlign.Right -> width - textPadding
            TextAlign.Middle -> width * 0.5f
            TextAlign.Left -> textPadding
        }
        text(
            displayString,
            x + textOrigin,
            y + h * 0.5f,
            ColorPalette.textColor,
            textScale,
            align = textAlign,
            verticalAlign = verticalAlign
        )
        GlStateManager.popMatrix()
    }
    private fun drawTextBoxNoBox(){
        GlStateManager.pushMatrix()
        val displayString = elementValue.ifBlank { placeHolder }
        val width = stringWidth(displayString, textScale, minWidth, textPadding)

        val boxOffset = when (textAlign) {
            TextAlign.Left -> 0f
            TextAlign.Middle -> -width * 0.5f
            TextAlign.Right -> -width
        }
        translate(boxOffset, 0f)

        val textOrigin = when (textAlign) {
            TextAlign.Right -> width - textPadding
            TextAlign.Middle -> width * 0.5f
            TextAlign.Left -> textPadding
        }
        text(
            displayString,
            x + textOrigin,
            y + h * 0.5f,
            ColorPalette.textColor,
            textScale,
            align = textAlign,
            verticalAlign = verticalAlign
        )
        GlStateManager.popMatrix()
    }


    fun resetSelection(){
        listeningTextSelection = false
        selectionStart = 0
        selectionEnd = 0
    }

}