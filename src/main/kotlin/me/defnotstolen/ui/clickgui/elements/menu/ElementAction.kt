package me.defnotstolen.ui.clickgui.elements.menu

import me.defnotstolen.features.settings.impl.ActionSetting
import me.defnotstolen.font.OdinFont
import me.defnotstolen.ui.clickgui.elements.Element
import me.defnotstolen.ui.clickgui.elements.ElementType
import me.defnotstolen.ui.clickgui.elements.ModuleButton
import me.defnotstolen.ui.clickgui.util.ColorUtil.darker
import me.defnotstolen.ui.clickgui.util.ColorUtil.elementBackground
import me.defnotstolen.ui.clickgui.util.ColorUtil.textColor
import me.defnotstolen.ui.util.MouseUtils
import me.defnotstolen.utils.render.TextAlign
import me.defnotstolen.utils.render.TextPos
import me.defnotstolen.utils.render.roundedRectangle
import me.defnotstolen.utils.render.text

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