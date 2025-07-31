package noobroutes.ui.clickgui.elements.menu

import noobroutes.features.settings.impl.DualSetting
import noobroutes.font.FontRenderer
import noobroutes.ui.ColorPalette.buttonColor
import noobroutes.ui.ColorPalette.clickGUIColor
import noobroutes.ui.ColorPalette.elementBackground
import noobroutes.ui.clickgui.elements.Element
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.ui.clickgui.elements.ModuleButton
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.animations.impl.EaseInOut
import noobroutes.utils.render.Color
import noobroutes.utils.render.ColorUtil.darker
import noobroutes.utils.render.ColorUtil.darkerIf
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
class ElementDual(parent: ModuleButton, setting: DualSetting) : Element<DualSetting>(
    parent, setting, ElementType.DUAL
) {
    private val posAnim = EaseInOut(250)

    private val isRightHovered: Boolean
        get() = MouseUtils.isAreaHovered(x + w * 0.5f + 5f, y + 2f, w * 0.5f - 10f, 30f)

    private val isLeftHovered: Boolean
        get() = MouseUtils.isAreaHovered(x + 5f, y + 2f, w * 0.5f - 10f, 30f)

    override fun draw() {
        roundedRectangle(x, y, w, h, elementBackground)
        roundedRectangle(x + 7f, y + 3f, w - 14f, 28f, buttonColor, 5f)

        val pos = posAnim.get(8f, w * 0.5f, !setting.enabled)
        roundedRectangle(x + pos, y + 3f, w * 0.5f - 6f, 28f, clickGUIColor.darker(0.8f), 5f)

        text(setting.left, x + w * 0.25f + 6f, y + 1f + h * 0.5f, Color.WHITE.darkerIf(isLeftHovered), 12f, FontRenderer.REGULAR, TextAlign.Middle)
        text(setting.right, x + w * 3 * 0.25f - 3f,y + 1f + h * 0.5f, Color.WHITE.darkerIf(isRightHovered), 12f, FontRenderer.REGULAR, TextAlign.Middle)
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton != 0) return false
        if (isLeftHovered && setting.enabled) {
            if (posAnim.start()) setting.enabled = false
            return true
        } else if (isRightHovered && !setting.enabled) {
            if (posAnim.start()) setting.enabled = true
            return true
        }
        return false
    }
}