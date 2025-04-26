package com.github.wadey3636.noobroutes.ui.clickgui.elements.menu

import com.github.wadey3636.noobroutes.font.OdinFont
import com.github.wadey3636.noobroutes.features.settings.impl.ActionSetting
import com.github.wadey3636.noobroutes.ui.clickgui.elements.Element
import com.github.wadey3636.noobroutes.ui.clickgui.elements.ElementType
import com.github.wadey3636.noobroutes.ui.clickgui.elements.ModuleButton
import com.github.wadey3636.noobroutes.ui.clickgui.util.ColorUtil.darker
import com.github.wadey3636.noobroutes.ui.clickgui.util.ColorUtil.elementBackground
import com.github.wadey3636.noobroutes.ui.clickgui.util.ColorUtil.textColor
import com.github.wadey3636.noobroutes.ui.util.MouseUtils
import com.github.wadey3636.noobroutes.utils.render.TextAlign
import com.github.wadey3636.noobroutes.utils.render.TextPos
import com.github.wadey3636.noobroutes.utils.render.roundedRectangle
import com.github.wadey3636.noobroutes.utils.render.text

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