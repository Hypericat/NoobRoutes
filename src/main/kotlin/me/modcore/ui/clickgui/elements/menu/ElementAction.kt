package me.modcore.ui.clickgui.elements.menu

import me.modcore.features.settings.impl.ActionSetting
import me.modcore.font.OdinFont
import me.modcore.ui.clickgui.elements.Element
import me.modcore.ui.clickgui.elements.ElementType
import me.modcore.ui.clickgui.elements.ModuleButton
import me.modcore.ui.clickgui.util.ColorUtil.darker
import me.modcore.ui.clickgui.util.ColorUtil.elementBackground
import me.modcore.ui.clickgui.util.ColorUtil.textColor
import me.modcore.ui.util.MouseUtils
import me.modcore.utils.render.TextAlign
import me.modcore.utils.render.TextPos
import me.modcore.utils.render.roundedRectangle
import me.modcore.utils.render.text

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
        text(name, x + w / 2f, y + h / 2f, if (isHovered) textColor.darker() else textColor, 12f , OdinFont.REGULAR, TextAlign.Middle, TextPos.Middle)
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton == 0 && isHovered) {
            setting.action()
            return true
        }
        return false
    }
}