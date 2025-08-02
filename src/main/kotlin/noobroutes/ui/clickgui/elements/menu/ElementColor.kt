package noobroutes.ui.clickgui.elements.menu

import net.minecraft.client.renderer.GlStateManager
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.font.FontRenderer
import noobroutes.ui.ColorPalette.TEXT_OFFSET
import noobroutes.ui.ColorPalette.elementBackground
import noobroutes.ui.ColorPalette.textColor
import noobroutes.ui.clickgui.elements.Element
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.utils.render.Color
import noobroutes.utils.render.roundedRectangle
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



    override fun draw() {
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)
        roundedRectangle(0f, 0f, w, h, elementBackground)
        text(name, TEXT_OFFSET,  18f, textColor, 12f, FontRenderer.REGULAR)
        GlStateManager.popMatrix()
    }
}