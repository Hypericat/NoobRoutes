package noobroutes.ui.editgui.elements

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.ColorPalette
import noobroutes.ui.ColorPalette.TEXT_OFFSET
import noobroutes.ui.editgui.EditGuiBase
import noobroutes.ui.editgui.EditGuiElement
import noobroutes.ui.util.UiElement
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.wrappedText
import noobroutes.utils.render.wrappedTextBounds

class EditGuiDescription(val text: String, val tiedElement: EditGuiElement? = null): UiElement(0f, 0f), EditGuiElement {
    override val height: Float = wrappedTextBounds(text, EditGuiBase.BUTTON_WIDTH, 16f).second + 25f
    override var priority: Int = -100
    override val isDoubleWidth: Boolean = true



    override fun draw() {
        GlStateManager.pushMatrix()
        translate(x + TEXT_OFFSET, y)
        roundedRectangle(0f, 0f, EditGuiBase.BUTTON_WIDTH, height + 15f, ColorPalette.elementBackground, 10f)
        wrappedText(text, 0f, 15f, EditGuiBase.BUTTON_WIDTH, ColorPalette.textColor, 16f)
        GlStateManager.popMatrix()
    }



}