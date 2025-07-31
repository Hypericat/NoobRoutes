package noobroutes.ui.clickgui.elements.menu

import noobroutes.features.settings.impl.ActionSetting
import noobroutes.ui.ColorPalette.elementBackground
import noobroutes.ui.clickgui.elements.Element
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.ui.clickgui.elements.Panel
import noobroutes.utils.render.roundedRectangle

/**
 * Renders all the modules.
 *
 * Backend made by Aton, with some changes
 * Design mostly made by Stivais
 *
 * @author Stivais, Aton
 * @see [Element]
 */
class ElementAction(setting: ActionSetting) : Element<ActionSetting>(setting, ElementType.ACTION) {
    override val isHovered: Boolean
        get() = isAreaHovered(x + 20f, y, w - 40f, h - 10f)

    override fun draw() {
        roundedRectangle(0f, y, Panel.WIDTH, h, elementBackground)
        /*
        text(name, w * 0.5, y + h * 0.5, if (isHovered) textColor.darker() else textColor, 12f , FontRenderer.REGULAR, TextAlign.Middle, TextPos.Middle)
         */
        //TODO
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton == 0 && isHovered) {
            setting.action()
            return true
        }
        return false
    }
}