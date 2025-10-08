package noobroutes.ui.editgui.elements

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.ColorPalette
import noobroutes.ui.ColorPalette.TEXT_OFFSET
import noobroutes.ui.ColorPalette.buttonColor
import noobroutes.ui.ColorPalette.elementBackground
import noobroutes.ui.editgui.EditGui
import noobroutes.ui.editgui.EditGuiBase
import noobroutes.ui.editgui.EditGuiElement
import noobroutes.ui.editgui.EditGuiPage
import noobroutes.ui.util.UiElement
import noobroutes.utils.render.ColorUtil.darkerIf
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.drawArrow
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.text
import noobroutes.utils.skyblock.modMessage

class EditGuiPageOpener(val page: EditGuiPage) : UiElement(0f, 0f), EditGuiElement {
    override val height: Float = 60f
    override val isDoubleWidth: Boolean = true
    override var priority: Int = 0

    private inline val isHovered get() = isAreaHovered(0f, 0f, BUTTON_WIDTH, 50f)
    private inline val color get() = buttonColor.darkerIf(isHovered, 0.7f)


    override fun draw() {
        GlStateManager.pushMatrix()
        translate(x + TEXT_OFFSET, y)
        roundedRectangle(0f, 0f, BUTTON_WIDTH, 50f, color, 10f)
        text(page.name, HALF_BUTTON_WIDTH, 25f, ColorPalette.textColor, 16f, align = TextAlign.Middle)
        drawArrow(BUTTON_WIDTH * 0.95f, 25f, rotation = 0f, scale = 1f)
        GlStateManager.popMatrix()
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton != 0 || !isHovered) return false
        EditGui.openPage(page)
        return true
    }




    companion object {
        private const val BUTTON_WIDTH = EditGuiBase.WIDTH - TEXT_OFFSET * 2f - 60f
        private const val HALF_BUTTON_WIDTH = BUTTON_WIDTH * 0.5f
    }
}