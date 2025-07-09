package noobroutes.ui.util.elements

import net.minecraft.client.renderer.GlStateManager
import noobroutes.Core.logger
import noobroutes.ui.ColorPalette
import noobroutes.ui.clickgui.ClickGUI.TEXTOFFSET
import noobroutes.ui.clickgui.util.ColorUtil.brighter
import noobroutes.ui.clickgui.util.ColorUtil.buttonColor
import noobroutes.ui.clickgui.util.ColorUtil.clickGUIColor
import noobroutes.ui.clickgui.util.ColorUtil.darkerIf
import noobroutes.ui.clickgui.util.HoverHandler
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.MouseUtils.isAreaHovered
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.utils.floor
import noobroutes.utils.render.Color
import noobroutes.utils.render.getTextWidth
import noobroutes.utils.render.rectangleOutline
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.skyblock.modMessage
import kotlin.math.roundToInt
import kotlin.text.ifEmpty

class SliderElement(
    name: String,
    x: Float,
    y: Float,
    h: Float,
    w: Float,
    override var elementValue: Double,
    val min: Double, val max: Double,
    val unit: String = ""
) : UiElement(name, x, y, h, w, ), ElementValue<Double> {

    override val elementValueChangeListeners = mutableListOf<(Double) -> Unit>()
    private val handler = HoverHandler(0, 150)
    private val colorAnim = ColorAnimation(100)
    private var listeningText = false
    private var listeningTextField: String = ""
    var listening = false

    private val isHoveredBox: Boolean
        get() = isAreaHovered(x + w - TEXTOFFSET - 30, y  + 5f, 32f, 21.5f )

    val isHovered: Boolean
        get() = isAreaHovered(x, y + 21.5f, w - 15f, 33.5f)

    private var sliderPercentage: Float = ((-min) / (max - min)).toFloat()

    private inline val color: Color
        get() = clickGUIColor.brighter(1 + handler.percent() / 200f)


    override fun draw() {
        drawName()
        drawSliderTextBox(
            getDisplay(),
            x + w * 0.9f,
            halfHeight,
            1f,
            1f,
            colorAnim.get(
                ColorPalette.elementPrimary,
                ColorPalette.backgroundSecondary.brighter(1.1f),
                !listening
            ).darkerIf(isHoveredBox)
        )
        
    }

    companion object {
        const val SLIDER_WIDTH = 100f
        const val SLIDER_HEIGHT = 7f
        const val HALF_SLIDER_WIDTH = SLIDER_WIDTH * 0.5f
        const val HALF_SLIDER_HEIGHT = SLIDER_HEIGHT * 0.5f
        val sliderBGColor = Color(-0xefeff0)

        fun drawSlider(x: Float, y: Float, xScale: Float, yScale: Float, percentage: Float, sliderColor: Color) {
            GlStateManager.pushMatrix()
            GlStateManager.translate(x, y, 1f)
            GlStateManager.scale(xScale, yScale, 1f)
            roundedRectangle(-HALF_SLIDER_WIDTH, -HALF_SLIDER_WIDTH, SLIDER_WIDTH, SLIDER_HEIGHT, sliderBGColor, 3f)
            roundedRectangle(-HALF_SLIDER_WIDTH, -HALF_SLIDER_WIDTH, percentage * SLIDER_WIDTH, 7f, sliderColor, 3f)

            GlStateManager.popMatrix()

        }
        const val TEXT_BOX_BASE_WIDTH = 32f
        const val TEXT_BOX_HEIGHT = 21.5f
        const val HALF_TEXT_BOX_HEIGHT = TEXT_BOX_HEIGHT * 0.5f

        fun drawSliderTextBox(display: String, x: Float, y: Float, xScale: Float, yScale: Float, color: Color) {
            GlStateManager.pushMatrix()
            GlStateManager.translate(x, y, 1f)
            GlStateManager.scale(xScale, yScale, 1f)
            val textWidth = getTextWidth(display, 12f)
            val halfWidth = (TEXT_BOX_BASE_WIDTH + textWidth) * 0.5f
            roundedRectangle(-halfWidth, -HALF_TEXT_BOX_HEIGHT, textWidth, TEXT_BOX_HEIGHT, buttonColor, 4f, edgeSoftness = 1f)
            rectangleOutline(-halfWidth, -HALF_TEXT_BOX_HEIGHT, textWidth, TEXT_BOX_HEIGHT, color,4f, 3f)
        }
    }

    private fun getDisplay(): String {
        if (listeningText) {
            return "${listeningTextField.ifEmpty { " " }}${unit}"
        }
        return if (elementValue - elementValue.floor() == 0.0) {
            "${(elementValue * 100.0).roundToInt() / 100}${unit}"
        } else {
            "${(elementValue * 100.0).roundToInt() / 100.0}${unit}"
        }
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (isHoveredBox && mouseButton == 0) {
            if (listeningText) {
                textUnlisten()
                return true
            }
            listeningText = true
            listeningTextField = elementValue.toInt().toString()
            return true
        }
        if (listeningText && mouseButton == 0) {
            textUnlisten()
            listeningText = false
            if (isHovered) {
                listening = true
            }
            return true
        }

        if (mouseButton == 0 && isHovered) {
            listening = true
            return true
        }
        return false
    }

    override fun mouseReleased(): Boolean {
        listening = false
        return false
    }
    override fun mouseClickedAnywhere(){
        if (listeningText && !isHovered && !isHoveredBox) {
            textUnlisten()
            return
        }
        return
    }
    private fun textUnlisten() {
        if (listeningTextField.isEmpty()) {
            elementValue = min
            sliderPercentage = ((elementValue - min) / (max - min)).toFloat()
            listeningText = false
            return
        }
        elementValue = try {
            listeningTextField.toDouble()
        } catch (e: NumberFormatException) {
            modMessage("Invalid number! Defaulting to previous value.")
            logger.error(listeningTextField, e)
            elementValue
        }
        sliderPercentage = ((elementValue - min) / (max - min)).toFloat()
        listeningText = false
    }

}