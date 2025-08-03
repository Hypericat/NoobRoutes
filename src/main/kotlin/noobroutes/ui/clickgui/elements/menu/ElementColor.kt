package noobroutes.ui.clickgui.elements.menu

import net.minecraft.client.renderer.GlStateManager
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.font.FontRenderer
import noobroutes.ui.ColorPalette
import noobroutes.ui.ColorPalette.TEXT_OFFSET
import noobroutes.ui.ColorPalette.elementBackground
import noobroutes.ui.ColorPalette.textColor
import noobroutes.ui.clickgui.elements.Element
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.ui.clickgui.elements.ModuleButton
import noobroutes.ui.clickgui.elements.Panel
import noobroutes.ui.util.animations.impl.CubicBezierAnimation
import noobroutes.ui.util.elements.textElements.NumberBoxElement
import noobroutes.ui.util.elements.textElements.TextBoxElement
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants.COLOR_BOX_SIZE
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants.COLOR_POPOUT_GAP
import noobroutes.ui.util.elements.colorelement.ColorElement.ColorElementsConstants.TEXT_BOX_HEIGHT
import noobroutes.ui.util.elements.colorelement.ColorPopoutElement
import noobroutes.utils.Utils.COLOR_NORMALIZER
import noobroutes.utils.render.Color
import noobroutes.utils.render.Color.Companion.HEX_REGEX
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.popStencil
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.stencilRoundedRectangle
import noobroutes.utils.render.text

/**
 * Renders all the modules.
 *
 * Backend made by Aton, with some changes
 * Design mostly made by Stivais
 *
 * @author Stivais, Aton
 * @see [Element]
 */
class ElementColor(setting: ColorSetting) :
    Element<ColorSetting>(setting, ElementType.COLOR) {
    inline val color: Color
        get() = setting.value

    companion object {
        private const val COLOR_ELEMENT_WIDTH = 34f
        private const val COLOR_ELEMENT_HEIGHT = 20f
        private const val COLOR_ELEMENT_WIDTH_HALF = COLOR_ELEMENT_WIDTH * 0.5f
        private const val COLOR_ELEMENT_HEIGHT_HALF = COLOR_ELEMENT_HEIGHT * 0.5f
        private const val COLOR_ELEMENT_RADIUS = 6f
        private const val COLOR_ELEMENT_X_POSITION = Panel.WIDTH - COLOR_ELEMENT_WIDTH - BORDER_OFFSET
        private const val COLOR_ELEMENT_Y_POSITION = ModuleButton.BUTTON_HEIGHT * 0.5f - COLOR_ELEMENT_HEIGHT_HALF
    }

    private inline val isHoveredColor
        get() = isAreaHovered(
            COLOR_ELEMENT_X_POSITION,
            COLOR_ELEMENT_Y_POSITION,
            COLOR_ELEMENT_WIDTH,
            COLOR_ELEMENT_HEIGHT
        )
    private val extendAnimation = CubicBezierAnimation(250, 0.4, 0, 0.2, 1)


    init {
        addChildren(
            TextBoxElement(
                "HEX",
                COLOR_POPOUT_GAP,
                0f,
                Panel.WIDTH - COLOR_POPOUT_GAP * 2f,
                TEXT_BOX_HEIGHT,
                12f, TextAlign.Middle, 5f, 6f,
                textColor, if (setting.allowAlpha) 8 else 6,
                TextBoxElement.TextBoxType.GAP,
                3f,
                color.hex,
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
            },

        )
    }



    override fun doHandleDraw() {
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
            stencilRoundedRectangle(0f, 0f, w, getHeight())
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
        return ModuleButton.BUTTON_HEIGHT + (COLOR_POPOUT_GAP * 2 + COLOR_BOX_SIZE + TEXT_BOX_HEIGHT * 2f) * extendAnimation.get(0f, 1f, !extended)
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