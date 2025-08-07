package noobroutes.ui.clickgui.elements.menu

import noobroutes.features.settings.impl.SelectorSetting
import noobroutes.font.FontRenderer
import noobroutes.ui.ColorPalette.TEXT_OFFSET
import noobroutes.ui.ColorPalette.buttonColor
import noobroutes.ui.ColorPalette.clickGUIColor
import noobroutes.ui.ColorPalette.elementBackground
import noobroutes.ui.ColorPalette.textColor
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.ui.clickgui.elements.SettingElement
import noobroutes.ui.util.animations.impl.CubicBezierAnimation
import noobroutes.utils.capitalizeFirst
import noobroutes.utils.render.*
import noobroutes.utils.render.ColorUtil.brighterIf
import noobroutes.utils.render.ColorUtil.darker

/**
 * Renders all the modules.
 *
 * Backend made by Aton, with some changes
 * Design mostly made by Stivais
 *
 * @author Stivais, Aton
 * @see [SettingElement]
 */
class SettingElementSelector(setting: SelectorSetting) :
    SettingElement<SelectorSetting>(setting, ElementType.SELECTOR) {

    override val isHovered: Boolean
        get() = isAreaHovered(x, y, w, DEFAULT_HEIGHT)

    val display: String
        inline get() = setting.selected

    inline val size: Int
        get () = setting.options.size

    private val settingAnim = CubicBezierAnimation(200, 0.4, 0, 0.2, 1)

    private val isSettingHovered: (Int) -> Boolean = {
        isAreaHovered(x, y + 38f + 32f * it, w, 32f)
    }

    private val color: Color
        get() = buttonColor.brighterIf(isHovered)

    override fun draw() {
        h = settingAnim.get(32f, size * 36f + DEFAULT_HEIGHT, !extended)

        roundedRectangle(x, y, w, h, elementBackground)
        val width = getTextWidth(display, 12f)

        roundedRectangle(x + w - 20f - width, y + 4f, width + 12f, 22f, color, 5f)

        text(name, x + TEXT_OFFSET, y + 16f, textColor, 12f, FontRenderer.REGULAR)
        text(display, x + w - 14f - width, y + 8f, textColor, 12f, FontRenderer.REGULAR, TextAlign.Left, TextPos.Top)

        if (!extended && !settingAnim.isAnimating()) return

        rectangleOutline(x + w - 20f - width, y + 4f, width + 12f, 22f, clickGUIColor, 5f, 1.5f)

        val scissor = scissor(getEffectiveX() + x, y + getEffectiveY(), w, h)

        roundedRectangle(x + TEXT_OFFSET, y + 37f, w - 12f, size * 32f, buttonColor, 5f)

        for (i in 0 until size) {
            val y = y + 38 + 32 * i
            text(setting.options[i].lowercase().capitalizeFirst(), x + w / 2f, y + 6f, textColor, 12f, FontRenderer.REGULAR, TextAlign.Middle, TextPos.Top)
            if (isSettingHovered(i)) rectangleOutline(x + 5, y - 1f, w - 11.5f, 32.5f, clickGUIColor.darker(), 4f, 3f)
        }
        resetScissor(scissor)
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton == 0) {
            if (isHovered) {
                if (settingAnim.start()) extended = !extended
                return true
            }
            if (!extended) return false
            for (index in 0 until setting.options.size) {
                if (isSettingHovered(index)) {
                    if (settingAnim.start()) {
                        setting.selected = setting.options[index]
                        extended = false
                    }
                    return true
                }
            }
        } else if (mouseButton == 1) {
            if (isHovered) {
                setting.index += 1
                return true
            }
        }
        return false
    }
}