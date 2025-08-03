package noobroutes.ui.clickgui.elements.menu

import net.minecraft.client.renderer.GlStateManager
import noobroutes.features.settings.impl.StringSetting
import noobroutes.ui.ColorPalette.TEXT_OFFSET
import noobroutes.ui.ColorPalette.elementBackground
import noobroutes.ui.ColorPalette.textColor
import noobroutes.ui.clickgui.elements.Element
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.ui.util.elements.textElements.TextBoxElement
import noobroutes.utils.render.ColorUtil.darker
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

    companion object {
        private const val TEXT_BOX_ELEMENT_HEIGHT = 24f
        private const val TEXT_BOX_ELEMENT_HEIGHT_HALF = TEXT_BOX_ELEMENT_HEIGHT * 0.5f
    }

    val textElement = TextBoxElement(
        "", w - BORDER_OFFSET, h * 0.5f - TEXT_BOX_ELEMENT_HEIGHT_HALF, 16f, TEXT_BOX_ELEMENT_HEIGHT, 12f, TextAlign.Right, 6f, 3f,
        textColor.darker(), 12, TextBoxElement.TextBoxType.NORMAL, 3f, setting.text
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
        GlStateManager.translate(x, y, 0f)
        roundedRectangle(0f, 0f, w, h, elementBackground)
        text(name, TEXT_OFFSET, h * 0.5f, textColor, 12f)
        GlStateManager.popMatrix()
    }



}