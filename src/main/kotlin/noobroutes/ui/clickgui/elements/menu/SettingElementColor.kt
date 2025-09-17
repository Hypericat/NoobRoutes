package noobroutes.ui.clickgui.elements.menu

import net.minecraft.client.renderer.GlStateManager
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.font.FontRenderer
import noobroutes.ui.ColorPalette
import noobroutes.ui.ColorPalette.TEXT_OFFSET
import noobroutes.ui.ColorPalette.elementBackground
import noobroutes.ui.ColorPalette.textColor
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.ui.clickgui.elements.ModuleButton
import noobroutes.ui.clickgui.elements.Panel
import noobroutes.ui.clickgui.elements.SettingElement
import noobroutes.ui.util.animations.impl.CubicBezierAnimation
import noobroutes.ui.util.elements.colorelement.AlphaSliderElement
import noobroutes.ui.util.elements.colorelement.ColorBoxElement
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants.COLOR_BOX_SIZE
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants.COLOR_POPOUT_GAP
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants.COLOR_POPOUT_GAP_THIRD
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants.TEXT_BOX_HEIGHT
import noobroutes.ui.util.elements.colorelement.ColorPopoutElement
import noobroutes.ui.util.elements.colorelement.ColorSliderElement
import noobroutes.ui.util.elements.textElements.AccessorBasedNumberBoxElement
import noobroutes.ui.util.elements.textElements.TextBoxElement
import noobroutes.utils.Utils.COLOR_NORMALIZER
import noobroutes.utils.render.*
import noobroutes.utils.render.Color.Companion.HEX_REGEX

/**
 * Renders all the modules.
 *
 * Backend made by Aton, with some changes
 * Design mostly made by Stivais
 *
 * @author Stivais, Aton
 * @see [SettingElement]
 */
class SettingElementColor(setting: ColorSetting) :
    SettingElement<ColorSetting>(setting, ElementType.COLOR) {
    inline val color: Color
        get() = setting.value

    companion object {
        private const val COLOR_ELEMENT_WIDTH = 34f
        private const val COLOR_ELEMENT_HEIGHT = 20f
        private const val COLOR_ELEMENT_HEIGHT_HALF = COLOR_ELEMENT_HEIGHT * 0.5f
        private const val COLOR_ELEMENT_RADIUS = 6f
        private const val COLOR_ELEMENT_X_POSITION = Panel.WIDTH - COLOR_ELEMENT_WIDTH - BORDER_OFFSET
        private const val COLOR_ELEMENT_Y_POSITION = ModuleButton.BUTTON_HEIGHT * 0.5f - COLOR_ELEMENT_HEIGHT_HALF
        private const val HEX_WIDTH = Panel.WIDTH - COLOR_POPOUT_GAP * 2f
        private const val RGB_BOX_WIDTH = (HEX_WIDTH - COLOR_POPOUT_GAP * 4f) / 3
        private const val RGB_BOX_GAP = (Panel.WIDTH - COLOR_POPOUT_GAP) / 3
        private const val SHIFT = 1f //I am shifting it over by 1 because of the green line, makes it look even.
        private const val GAP = (Panel.WIDTH - COLOR_POPOUT_GAP * 2f - COLOR_BOX_SIZE - ColorElementsConstants.COLOR_SLIDER_WIDTH * 2f) / 3f
        private const val COLOR_SLIDER_X_POSITION = COLOR_POPOUT_GAP * 2f + COLOR_BOX_SIZE + ColorElementsConstants.COLOR_SLIDER_WIDTH_HALF
        private const val ALPHA_SLIDER_X_POSITION = COLOR_POPOUT_GAP * 3f + COLOR_BOX_SIZE + ColorElementsConstants.COLOR_SLIDER_WIDTH * 1.5f
    }


    private inline val isHoveredColor
        get() = isAreaHovered(
            COLOR_ELEMENT_X_POSITION,
            COLOR_ELEMENT_Y_POSITION,
            COLOR_ELEMENT_WIDTH,
            COLOR_ELEMENT_HEIGHT
        )
    private val extendAnimation = CubicBezierAnimation(250, 0.4, 0, 0.2, 1)

    val hexElement = TextBoxElement(
        "HEX",
        COLOR_POPOUT_GAP + HEX_WIDTH * 0.5f + SHIFT,
        COLOR_POPOUT_GAP * 3f + TEXT_BOX_HEIGHT + COLOR_BOX_SIZE + ModuleButton.BUTTON_HEIGHT,
        HEX_WIDTH,
        TEXT_BOX_HEIGHT,
        12f, TextAlign.Middle, 5f, 6f,
        textColor, if (setting.allowAlpha) 8 else 6,
        TextBoxElement.TextBoxType.GAP,
        2f,
        getHex(),
    ).apply {
        addValueChangeListener {
            if (!HEX_REGEX.matches(it)) {
                elementValue = (parent as? ColorPopoutElement)?.elementValue?.hex ?: return@addValueChangeListener
                return@addValueChangeListener
            }
            color.r = it.substring(0, 2).toInt(16)
            color.g = it.substring(2, 4).toInt(16)
            color.b = it.substring(4, 6).toInt(16)
            color.alpha = if (it.length == 8) it.substring(6, 8).toInt(16) * COLOR_NORMALIZER else 1f

        }
    }
    val rgbElements = listOf("R", "G", "B").mapIndexed { index, label ->

        val y = ModuleButton.BUTTON_HEIGHT + COLOR_POPOUT_GAP * 2f + COLOR_BOX_SIZE
        val x = COLOR_POPOUT_GAP + RGB_BOX_GAP * index + SHIFT + COLOR_POPOUT_GAP_THIRD * (-1 * (-index + 1))

        /*
                    for (i in rgbElements.indices) {
                rgbElements[i].updatePosition(
                    COLOR_POPOUT_GAP + RGB_BOX_GAP * i + SHIFT + COLOR_POPOUT_GAP_THIRD * (-1 * (-i + 1)),
                    COLOR_POPOUT_GAP * 2f + COLOR_BOX_SIZE + ModuleButton.BUTTON_HEIGHT
                )
            }
         */

        AccessorBasedNumberBoxElement(
            label, x, y,
            RGB_BOX_WIDTH, ColorElementsConstants.TEXT_BOX_HEIGHT,
            12f, TextAlign.Left, 5f, 6f,
            ColorPalette.textColor,
            3,
            TextBoxElement.TextBoxType.GAP,
            2f,
            0,
            0.0,
            255.0,
            when (index) {
                0 -> {{ color.r.toDouble() }}
                1 -> {{color.g.toDouble()}}
                else -> {{color.b.toDouble()}}
            },
            when (index) {
                0 -> {{
                    color.r = it.toInt()
                    updateHexElement()
                }}
                1 -> {{
                    color.g = it.toInt()
                    updateHexElement()
                }}
                else -> {{
                    color.b = it.toInt()
                    updateHexElement()
                }}
            }
        )
    }
    private val colorBox = ColorBoxElement(
        GAP + SHIFT + ColorElementsConstants.COLOR_BOX_SIZE_HALF,
        ColorElementsConstants.COLOR_BOX_SIZE_HALF + ModuleButton.BUTTON_HEIGHT + COLOR_POPOUT_GAP,
        color
    ).apply { addValueChangeListener { updateHexElement() } }
    private val colorSlider = ColorSliderElement(
        COLOR_SLIDER_X_POSITION,
        ColorElementsConstants.COLOR_BOX_SIZE_HALF + ModuleButton.BUTTON_HEIGHT + COLOR_POPOUT_GAP,
        color
    ).apply { addValueChangeListener { updateHexElement() } }
    private val alphaSlider = if (setting.allowAlpha) AlphaSliderElement(
        ALPHA_SLIDER_X_POSITION,
        ColorElementsConstants.COLOR_BOX_SIZE_HALF + ModuleButton.BUTTON_HEIGHT + COLOR_POPOUT_GAP,
        color
    ).apply { addValueChangeListener { updateHexElement() } } else null

    fun updateHexElement(){
        hexElement.elementValue = getHex()
    }

    private fun getHex(): String{
        return if (setting.allowAlpha) color.hex else color.hex.dropLast(2)
    }

    init {
        addChildren(hexElement)
        addChildren(rgbElements)
        addChildren(colorBox, colorSlider)
        alphaSlider?.let { addChild(it) }
    }

    override fun doHandleDraw() {
        if (!visible) return
        GlStateManager.pushMatrix()
        translate(x, y)
        roundedRectangle(0f, 0f, w, getHeight(), elementBackground)
        text(name, TEXT_OFFSET,  18f, textColor, 12f, FontRenderer.REGULAR)
        roundedRectangle(
            COLOR_ELEMENT_X_POSITION,
            COLOR_ELEMENT_Y_POSITION,
            COLOR_ELEMENT_WIDTH,
            COLOR_ELEMENT_HEIGHT,
            color,
            ColorPalette.buttonColor,
            Color.TRANSPARENT,
            3f,
            COLOR_ELEMENT_RADIUS,
            COLOR_ELEMENT_RADIUS,
            COLOR_ELEMENT_RADIUS,
            COLOR_ELEMENT_RADIUS,
            0.5f
        )
        if (extended || extendAnimation.isAnimating()) {
            roundedRectangle(0f, ModuleButton.BUTTON_HEIGHT, w, getHeight() - ModuleButton.BUTTON_HEIGHT, elementBackground, 15f)
            stencilRoundedRectangle(2f, 0f, w, getHeight(), 0f, 0f, 0f, 0f, 0.5f, false)


            for (i in uiChildren.indices) {
                uiChildren[i].apply {
                    visible = true
                    doHandleDraw()
                }
            }
            popStencil()
        } else {
            for (i in uiChildren.indices) {
                uiChildren[i].apply {
                    visible = false
                }
            }
        }

        GlStateManager.popMatrix()
    }

    override fun getHeight(): Float {
        return ModuleButton.BUTTON_HEIGHT + (COLOR_POPOUT_GAP * 4 + COLOR_BOX_SIZE + TEXT_BOX_HEIGHT * 2f) * extendAnimation.get(0f, 1f, !extended)
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton != 0) return false
        if (isHoveredColor) {
            if (extendAnimation.start()) extended = !extended
            return true
        }
        return false
    }
}