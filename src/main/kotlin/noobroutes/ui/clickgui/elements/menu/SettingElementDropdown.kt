package noobroutes.ui.clickgui.elements.menu

import net.minecraft.client.renderer.GlStateManager
import noobroutes.features.settings.impl.DropdownSetting
import noobroutes.font.FontRenderer
import noobroutes.ui.ColorPalette.TEXT_OFFSET
import noobroutes.ui.ColorPalette.elementBackground
import noobroutes.ui.ColorPalette.textColor
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.ui.clickgui.elements.SettingElement
import noobroutes.ui.util.animations.impl.LinearAnimation
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
 * @see [SettingElement]
 */
class SettingElementDropdown(setting: DropdownSetting) : SettingElement<DropdownSetting>(
    setting, ElementType.DROPDOWN
) {
    private val linearAnimation = LinearAnimation<Float>(200)

    override fun draw() {
        GlStateManager.pushMatrix()
        translate(x , y)
        roundedRectangle(0f, 0f, w, h, elementBackground)
        text(name, TEXT_OFFSET, h  * 0.5f, textColor, 12f, FontRenderer.REGULAR)

        val rotation = linearAnimation.get(0f, 90f, !setting.value)
        drawArrow(w - 12f, 16f, rotation, scale = 0.8f)
        GlStateManager.popMatrix()
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (isHovered) {
            if (linearAnimation.start()) {
                setting.enabled = !setting.enabled
                return true
            }
        }
        return false
    }

}