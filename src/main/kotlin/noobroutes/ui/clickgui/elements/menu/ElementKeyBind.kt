package noobroutes.ui.clickgui.elements.menu

import net.minecraft.client.renderer.GlStateManager
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.font.FontRenderer
import noobroutes.ui.ColorPalette.TEXT_OFFSET
import noobroutes.ui.ColorPalette.elementBackground
import noobroutes.ui.ColorPalette.textColor
import noobroutes.ui.clickgui.elements.Element
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.ui.util.elements.KeybindElement
import noobroutes.utils.render.TextAlign
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
class ElementKeyBind(setting: KeybindSetting) :
    Element<KeybindSetting>(setting, ElementType.KEY_BIND) {

    val keybind = KeybindElement(setting.value, w - BORDER_OFFSET, h * 0.5f, 1f, 1f, TextAlign.Right).apply {
        addValueChangeListener {
            setting.value = it
        }
    }
    init {
        addChild(keybind)
    }


    override fun draw() {
        GlStateManager.pushMatrix()
        roundedRectangle(x, y, w, h, elementBackground)
        text(name,  x + TEXT_OFFSET, h * 0.5f, textColor, 12f, FontRenderer.REGULAR)
        GlStateManager.popMatrix()
    }
}