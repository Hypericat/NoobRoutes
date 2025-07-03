package noobroutes.ui.clickgui.elements.menu

import noobroutes.features.settings.impl.DropdownSetting
import noobroutes.font.Font
import noobroutes.font.fonts.MinecraftFont
import noobroutes.ui.clickgui.ClickGUI.TEXTOFFSET
import noobroutes.ui.util.animations.impl.LinearAnimation
import noobroutes.ui.clickgui.elements.Element
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.ui.clickgui.elements.ModuleButton
import noobroutes.ui.clickgui.util.ColorUtil.elementBackground
import noobroutes.ui.clickgui.util.ColorUtil.textColor
import noobroutes.ui.util.MouseUtils.isAreaHovered
import noobroutes.utils.render.drawArrow
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
class ElementDropdown(parent: ModuleButton, setting: DropdownSetting) : Element<DropdownSetting>(
    parent, setting, ElementType.DROPDOWN
) {
    private val linearAnimation = LinearAnimation<Float>(200)

    override val isHovered: Boolean get() =
        isAreaHovered(x, y, w, h)

    override fun draw() {
        roundedRectangle(x, y, w, h, elementBackground)
        text(name, x + TEXTOFFSET, y + h / 2f, textColor, 12f, Font.REGULAR)

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