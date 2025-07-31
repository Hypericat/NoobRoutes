package noobroutes.ui.clickgui.elements.menu

import net.minecraft.client.renderer.GlStateManager
import noobroutes.features.settings.impl.DualSetting
import noobroutes.ui.ColorPalette.elementBackground
import noobroutes.ui.clickgui.elements.Element
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.ui.util.elements.DualElement
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
class ElementDual(setting: DualSetting) : Element<DualSetting>(
    setting, ElementType.DUAL
) {
    val dualElement = DualElement(setting.left, setting.right, w * 0.5f - DualElement.DUAL_ELEMENT_HALF_WIDTH, h * 0.5f, 1f, 1f, setting.enabled).apply {
        addValueChangeListener { setting.enabled = it }
    }

    init {
        addChild(dualElement)
    }

    override fun draw() {
        GlStateManager.pushMatrix()
        translate(x, y)
        roundedRectangle(0f, 0f, w, h, elementBackground)
        GlStateManager.popMatrix()
    }
}