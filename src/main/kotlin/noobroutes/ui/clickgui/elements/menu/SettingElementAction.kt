package noobroutes.ui.clickgui.elements.menu

import noobroutes.features.settings.impl.ActionSetting
import noobroutes.font.FontRenderer
import noobroutes.ui.ColorPalette.elementBackground
import noobroutes.ui.ColorPalette.textColor
import noobroutes.ui.clickgui.elements.SettingElement
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.utils.render.ColorUtil.darker
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.TextPos
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.text

/**
 * Renders all the modules.
 *
 * Backend made by Aton, with some changes
 * Design mostly made by Stivais
 *
 * @author Stivais, Aton
 * @see [SettingElement]
 */
class SettingElementAction(setting: ActionSetting) : SettingElement<ActionSetting>(setting, ElementType.ACTION) {
    override val isHovered: Boolean
        get() = isAreaHovered(x + 20f, y, w - 40f, h - 10f)

    override fun draw() {
        roundedRectangle(x, y, w, h, elementBackground)
        text(name, x + w * 0.5, y + h * 0.5, if (isHovered) textColor.darker() else textColor, 12f , FontRenderer.REGULAR, TextAlign.Middle, TextPos.Middle)


    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton == 0 && isHovered) {
            setting.action()
            return true
        }
        return false
    }
}