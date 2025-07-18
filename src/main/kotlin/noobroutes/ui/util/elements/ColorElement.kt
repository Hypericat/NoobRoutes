package noobroutes.ui.util.elements

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.DynamicTexture
import noobroutes.ui.ColorPalette
import noobroutes.ui.clickgui.util.ColorUtil.hsbMax
import noobroutes.ui.clickgui.util.ColorUtil.withAlpha
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.UiElement
import noobroutes.utils.render.Color
import noobroutes.utils.render.GradientDirection
import noobroutes.utils.render.RenderUtils.loadBufferedImage
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.circle
import noobroutes.utils.render.drawDynamicTexture
import noobroutes.utils.render.drawHSBBox
import noobroutes.utils.render.gradientRect
import noobroutes.utils.render.popStencil
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.stencilRoundedRectangle


//Make it so you can use hex code

class ColorElement(
    x: Float,
    y: Float,
    val w: Float,
    val h: Float,
    val radius: Float,
    override var elementValue: Color,
    val alphaEnabled: Boolean
) : UiElement(x, y), ElementValue<Color> {

    override val elementValueChangeListeners = mutableListOf<(Color) -> Unit>()
    var isOpen = false
    inline val isHovered get() = MouseUtils.isAreaHovered(x, y, w, h)

    private val popupWidth by lazy {
        COLOR_POPOUT_WIDTH + if (alphaEnabled) COLOR_POPOUT_ALPHA_WIDTH else 0f
    }

    private val rgbTextElements by lazy {
        listOf("R", "G", "B").mapIndexed { index, label ->
            TextBoxElement(
                label, 0f, 0f,
                TEXT_BOX_WIDTH, TEXT_BOX_HEIGHT,
                12f, TextAlign.Left, 5f, 6f,
                ColorPalette.text,
                3,
                TextBoxElement.TextBoxType.GAP,
                when (index) {
                    0 -> elementValue.r.toString()
                    1 -> elementValue.g.toString()
                    else -> elementValue.b.toString()
                }
            )
        }
    }
    private val hexElement by lazy {
        TextBoxElement(
            "HEX", 0f, 0f,
            popupWidth - COLOR_POPOUT_GAP * 3f + 3f,
            TEXT_BOX_HEIGHT,
            12f, TextAlign.Middle, 5f, 6f,
            ColorPalette.text, 3,
            TextBoxElement.TextBoxType.GAP,
            elementValue.hex,

        )
    }

    companion object {
        const val COLOR_POPOUT_GAP = 15f

        const val COLOR_BOX_SIZE = 150f
        const val COLOR_BOX_SIZE_HALF = COLOR_BOX_SIZE * 0.5f
        const val COLOR_BOX_RADIUS = 10f
        const val COLOR_BOX_CIRCLE_RADIUS = 9f
        const val COLOR_BOX_CIRCLE_THICKNESS = COLOR_BOX_CIRCLE_RADIUS * 0.225f

        val ALPHA_BACKGROUND = DynamicTexture(loadBufferedImage("/assets/ui/alphaBackground.png"))
        val HUE_GRADIENT = DynamicTexture(loadBufferedImage("/assets/ui/HueGradient.png"))
        const val COLOR_SLIDER_HEIGHT = 150f
        const val COLOR_SLIDER_WIDTH = 14f
        const val COLOR_SLIDER_HEIGHT_HALF = COLOR_SLIDER_HEIGHT * 0.5f
        const val COLOR_SLIDER_WIDTH_HALF = COLOR_SLIDER_WIDTH * 0.5f
        const val COLOR_SLIDER_CIRCLE_RADIUS = COLOR_SLIDER_WIDTH * 0.55f
        const val COLOR_SLIDER_CIRCLE_BORDER_THICKNESS = COLOR_SLIDER_CIRCLE_RADIUS * 0.3f

        const val COLOR_POPOUT_WIDTH = COLOR_POPOUT_GAP * 3f + COLOR_BOX_SIZE + COLOR_SLIDER_WIDTH
        const val COLOR_POPOUT_ALPHA_WIDTH = COLOR_POPOUT_GAP + COLOR_SLIDER_WIDTH

        const val TEXT_BOX_HEIGHT = 35f
        const val TEXT_BOX_THICKNESS = 3f

        //idk why I had to subtract 4, but I did
        const val TEXT_BOX_WIDTH = (COLOR_BOX_SIZE + COLOR_SLIDER_WIDTH * 2  - COLOR_POPOUT_GAP * 2f) / 3
        const val TEXT_BOX_WIDTH_WITH_GAP = (COLOR_BOX_SIZE + COLOR_SLIDER_WIDTH * 2 - TEXT_BOX_THICKNESS) / 3
        const val COLOR_POPOUT_GAP_THIRD = COLOR_POPOUT_GAP / 3

        const val COLOR_POPOUT_HEIGHT = COLOR_BOX_SIZE + COLOR_POPOUT_GAP * 4f + TEXT_BOX_HEIGHT * 2

        fun drawColorPickerPopup(x: Float, y: Float, scale: Float, color: Color, alphaEnabled: Boolean, textBoxes: List<TextBoxElement>, hexElement: TextBoxElement) {
            GlStateManager.pushMatrix()
            GlStateManager.translate(x, y, 1f)
            GlStateManager.scale(scale, scale, 1f)
            val width = COLOR_POPOUT_WIDTH + if (alphaEnabled) COLOR_POPOUT_ALPHA_WIDTH else 0f
            val topRX = width * -0.5f
            val topRY = COLOR_POPOUT_HEIGHT * -0.5f
            roundedRectangle(
                topRX,
                topRY,
                width,
                COLOR_POPOUT_HEIGHT,
                ColorPalette.backgroundPrimary,
                ColorPalette.backgroundSecondary,
                Color.TRANSPARENT,
                5f,
                10f,
                10f,
                10f,
                10f,
                0.5f
            )

            drawColorBox(topRX + COLOR_POPOUT_GAP + COLOR_BOX_SIZE_HALF, topRY + COLOR_POPOUT_GAP + COLOR_BOX_SIZE_HALF, 1f, color)
            if (alphaEnabled) {
                drawAlphaSlider(
                    topRX + COLOR_BOX_SIZE + COLOR_POPOUT_GAP * 3f + COLOR_SLIDER_WIDTH * 1.5f,
                    topRY + COLOR_POPOUT_GAP + COLOR_BOX_SIZE_HALF,
                    1f,
                    color
                )
            }
            drawColorSlider(topRX + COLOR_BOX_SIZE + COLOR_POPOUT_GAP * 2f + COLOR_SLIDER_WIDTH_HALF, topRY + COLOR_POPOUT_GAP + COLOR_BOX_SIZE_HALF, 1f, color)
            textBoxes.forEachIndexed { index, element ->
                element.updatePosition(
                    topRX + COLOR_POPOUT_GAP * (index + 1) + TEXT_BOX_WIDTH_WITH_GAP * index,
                    topRY + COLOR_POPOUT_GAP * 2f + COLOR_BOX_SIZE
                )
                element.draw()
            }
            hexElement.updatePosition(topRX + COLOR_POPOUT_GAP, topRY + COLOR_POPOUT_GAP * 3f + COLOR_BOX_SIZE + TEXT_BOX_HEIGHT)
            hexElement.draw()

            GlStateManager.popMatrix()
        }

        fun drawColorBox(x: Float, y: Float, scale: Float, color: Color){
            GlStateManager.pushMatrix()
            GlStateManager.translate(x, y, 1f)
            GlStateManager.scale(scale, scale, 1f)
            stencilRoundedRectangle(
                -COLOR_BOX_SIZE_HALF,
                -COLOR_BOX_SIZE_HALF,
                COLOR_BOX_SIZE,
                COLOR_BOX_SIZE,
                COLOR_BOX_RADIUS
            )
            drawHSBBox(-COLOR_BOX_SIZE_HALF, -COLOR_BOX_SIZE_HALF, COLOR_BOX_SIZE, COLOR_BOX_SIZE, color.hsbMax())

            circle(
                color.saturation * COLOR_SLIDER_WIDTH,
                (1 - color.brightness) * COLOR_SLIDER_WIDTH,
                COLOR_BOX_CIRCLE_RADIUS,
                Color.TRANSPARENT,
                Color.WHITE,
                COLOR_BOX_CIRCLE_THICKNESS
            )

            popStencil()
            GlStateManager.popMatrix()
        }

        fun drawColorSlider(x: Float, y: Float, scale: Float, color: Color){
            GlStateManager.pushMatrix()
            GlStateManager.translate(x, y, 0f)
            GlStateManager.scale(scale, scale, 1f)
            stencilRoundedRectangle(-COLOR_SLIDER_WIDTH_HALF, -COLOR_SLIDER_HEIGHT_HALF, COLOR_SLIDER_WIDTH, COLOR_SLIDER_HEIGHT, COLOR_SLIDER_CIRCLE_RADIUS)
            //rotates 270 to account for the png being horizontal
            GlStateManager.rotate(270f, 0f, 0f, 1f)
            drawDynamicTexture(HUE_GRADIENT, -COLOR_SLIDER_HEIGHT_HALF, -COLOR_SLIDER_WIDTH_HALF, COLOR_SLIDER_HEIGHT, COLOR_SLIDER_WIDTH)
            GlStateManager.rotate(-270f, 0f, 0f, -1f)
            circle(
                0f,
                -COLOR_SLIDER_HEIGHT_HALF + COLOR_SLIDER_HEIGHT * color.hue,
                COLOR_SLIDER_CIRCLE_RADIUS,
                Color.TRANSPARENT,
                Color.WHITE,
                COLOR_SLIDER_CIRCLE_BORDER_THICKNESS
            )
            popStencil()
            GlStateManager.popMatrix()
        }

        const val ALPHA_BACKGROUND_WIDTH = 20f
        const val ALPHA_BACKGROUND_HEIGHT = 155f
        const val ALPHA_BACKGROUND_WIDTH_HALF = ALPHA_BACKGROUND_WIDTH * 0.5f
        const val ALPHA_BACKGROUND_HEIGHT_HALF = ALPHA_BACKGROUND_HEIGHT * 0.5f

        fun drawAlphaSlider(x: Float, y: Float, scale: Float, color: Color){
            GlStateManager.pushMatrix()
            GlStateManager.translate(x, y, 0f)
            GlStateManager.scale(scale, scale, 1f)
            stencilRoundedRectangle(-COLOR_SLIDER_WIDTH_HALF, -COLOR_SLIDER_HEIGHT_HALF, COLOR_SLIDER_WIDTH, COLOR_SLIDER_HEIGHT, COLOR_SLIDER_CIRCLE_RADIUS)

            //the alpha background const values are based off of the png size,
            //it will get cut by the stencil tool to match the actual wanted size
            drawDynamicTexture(ALPHA_BACKGROUND, -ALPHA_BACKGROUND_WIDTH_HALF, -ALPHA_BACKGROUND_HEIGHT_HALF, ALPHA_BACKGROUND_WIDTH, ALPHA_BACKGROUND_HEIGHT)
            gradientRect(-COLOR_SLIDER_WIDTH_HALF, -COLOR_SLIDER_HEIGHT_HALF, COLOR_SLIDER_WIDTH, COLOR_SLIDER_HEIGHT, Color.TRANSPARENT, color.withAlpha(1f), 0f, GradientDirection.Up)
            circle(
                0f,
                -COLOR_SLIDER_HEIGHT_HALF + COLOR_SLIDER_HEIGHT - COLOR_SLIDER_HEIGHT * color.alpha,
                COLOR_SLIDER_CIRCLE_RADIUS,
                Color.TRANSPARENT,
                Color.WHITE,
                COLOR_SLIDER_CIRCLE_BORDER_THICKNESS
            )
            popStencil()
            GlStateManager.popMatrix()
        }

    }



    override fun draw() {
        roundedRectangle(
            x,
            y,
            w,
            h,
            elementValue,
            ColorPalette.backgroundSecondary,
            Color.TRANSPARENT,
            TEXT_BOX_THICKNESS,
            radius,
            radius,
            radius,
            radius,
            0.5f
        )
        if (isOpen) {
            drawColorPickerPopup(
                x + w * 2 + popupWidth, y, 1f, elementValue, alphaEnabled, rgbTextElements, hexElement
            )
        }
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (isHovered) {
            isOpen = !isOpen
            return true
        }
        return false
    }


}