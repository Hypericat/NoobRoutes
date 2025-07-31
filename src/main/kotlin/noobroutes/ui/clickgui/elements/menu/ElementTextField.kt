package noobroutes.ui.clickgui.elements.menu

import net.minecraft.client.renderer.GlStateManager
import noobroutes.features.settings.impl.StringSetting
import noobroutes.ui.ColorPalette.TEXT_OFFSET
import noobroutes.ui.ColorPalette.elementBackground
import noobroutes.ui.ColorPalette.textColor
import noobroutes.ui.clickgui.elements.Element
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.ui.util.elements.TextBoxElement
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
class ElementTextField(setting: StringSetting) :
    Element<StringSetting>(setting, ElementType.TEXT_FIELD) {

    val textElement = TextBoxElement(
        "", w - BORDER_OFFSET, h * 0.5f, 16f, 28f, 12f, TextAlign.Right, 6f, 3f,
        textColor, 12, TextBoxElement.TextBoxType.NORMAL, setting.text
    ).apply {
        addValueChangeListener {
            setting.text = it
        }
    }

    init {
        addChild(textElement)
    }

    override fun draw() {
        GlStateManager.pushMatrix()
        translate(x, y)
        roundedRectangle(0f, 0f, w, h, elementBackground)
        text(name, TEXT_OFFSET, h * 0.5f, textColor, 12f)
        GlStateManager.popMatrix()
    }



}