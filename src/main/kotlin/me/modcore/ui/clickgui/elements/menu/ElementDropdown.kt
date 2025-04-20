package me.modcore.ui.clickgui.elements.menu

import me.modcore.features.settings.impl.DropdownSetting
import me.modcore.font.OdinFont
import me.modcore.ui.clickgui.ClickGUI.TEXTOFFSET
import me.modcore.ui.clickgui.animations.impl.LinearAnimation
import me.modcore.ui.clickgui.elements.Element
import me.modcore.ui.clickgui.elements.ElementType
import me.modcore.ui.clickgui.elements.ModuleButton
import me.modcore.ui.clickgui.util.ColorUtil.elementBackground
import me.modcore.ui.clickgui.util.ColorUtil.textColor
import me.modcore.ui.util.MouseUtils.isAreaHovered
import me.modcore.utils.render.drawArrow
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
class ElementDropdown(parent: ModuleButton, setting: DropdownSetting) : Element<DropdownSetting>(
    parent, setting, ElementType.DROPDOWN
) {
    private val linearAnimation = LinearAnimation<Float>(200)

    override val isHovered: Boolean get() =
        isAreaHovered(x, y, w, h)

    override fun draw() {
        roundedRectangle(x, y, w, h, elementBackground)
        text(name, x + TEXTOFFSET, y + h / 2f, textColor, 12f, OdinFont.REGULAR)

        val rotation = linearAnimation.get(90f, 0f  , !setting.value)
        drawArrow(x + w - 12f, y + 16, rotation, scale = 0.8f)
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (isHovered) {
            if (linearAnimation.start()) {
                setting.enabled = !setting.enabled
                parent.updateElements()
                return true
            }
        }
        return false
    }

}