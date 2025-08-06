package noobroutes.ui.clickgui.elements.menu
import net.minecraft.client.renderer.GlStateManager
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.font.FontRenderer
import noobroutes.ui.ColorPalette.TEXT_OFFSET
import noobroutes.ui.ColorPalette.elementBackground
import noobroutes.ui.ColorPalette.textColor
import noobroutes.ui.clickgui.elements.Element
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.ui.util.elements.SwitchElement
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
class ElementSwitch(setting: BooleanSetting) : Element<BooleanSetting>(
    setting, ElementType.CHECK_BOX
) {

    val switchElement =
        SwitchElement(1f, setting.enabled, w - SwitchElement.SWITCH_WIDTH_HALF - BORDER_OFFSET, h * 0.5f).apply {
            addValueChangeListener {
                setting.enabled = it
            }
        }

    init {
        addChild(switchElement)
    }

    override fun draw() {
        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0f)
        roundedRectangle(0f, 0f, w, h, elementBackground)
        text(name, TEXT_OFFSET, h * 0.5, textColor, 12f, FontRenderer.REGULAR)
        GlStateManager.popMatrix()
    }
}