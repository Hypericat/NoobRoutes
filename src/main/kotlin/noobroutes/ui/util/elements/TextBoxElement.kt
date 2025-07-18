package noobroutes.ui.util.elements

import net.minecraft.client.renderer.GlStateManager
import noobroutes.font.FontType
import noobroutes.ui.ColorPalette
import noobroutes.ui.util.ElementValue
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

    override val elementValueChangeListeners = mutableListOf<(String) -> Unit>()
    override fun draw() {
        drawTextBoxWithGapTitle(elementValue, name, x, y, minWidth, h, radius, textScale, textPadding, boxColor, textAlign)
    }

    var listening = false

    override fun mouseClicked(mouseButton: Int): Boolean {
        return false

    }


    companion object {
        const val TEXT_BOX_THICKNESS = 3f
        const val TEXT_BOX_GAP_TEXT_MULTIPLIER = 0.1f
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
            val width = getTextWidth(text, textScale).coerceAtLeast(minWidth) + textPadding * 2
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
        }
    }

}