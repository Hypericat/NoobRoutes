package noobroutes.ui.clickgui.elements.menu

import noobroutes.features.settings.impl.ActionSetting
import noobroutes.font.Font
import noobroutes.ui.clickgui.elements.Element
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.ui.clickgui.elements.ModuleButton
import noobroutes.ui.clickgui.util.ColorUtil.darker
import noobroutes.ui.clickgui.util.ColorUtil.elementBackground
import noobroutes.ui.clickgui.util.ColorUtil.textColor
import noobroutes.ui.util.MouseUtils
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
 * @see [Element]
 */
class ElementAction(parent: ModuleButton, setting: ActionSetting) : Element<ActionSetting>(parent, setting, ElementType.ACTION) {
    override val isHovered: Boolean
        get() = MouseUtils.isAreaHovered(x + 20f, y, w - 40f, h - 10f)

// todo: improve this
    override fun draw() {
        roundedRectangle(x, y, w, h, elementBackground)
        text(name, x + w * 0.5, y + h * 0.5, if (isHovered) textColor.darker() else textColor, 12f , Font.REGULAR, TextAlign.Middle, TextPos.Middle)
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton == 0 && isHovered) {
            setting.action()
            return true
        }
        return false
    }
}