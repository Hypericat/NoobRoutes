package noobroutes.ui.editgui

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.editgui.EditGuiBase.Companion.X_ALIGNMENT_LEFT
import noobroutes.ui.editgui.EditGuiBase.Companion.X_ALIGNMENT_RIGHT
import noobroutes.ui.util.UiElement
import noobroutes.utils.skyblock.modMessage

class EditGuiPage(val name: String) : UiElement(0f, 0f) {
    var height = 0f

    fun addElement(uiElement: UiElement) {
        uiChildren.add(uiElement)
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        return false
    }


    override fun draw() {
        GlStateManager.pushMatrix()
        translate(x, y)
        GlStateManager.popMatrix()
    }

    fun updateYPositions(){
        var currentY = 80f
        var currentSide = 0
        var previousHeight = 0f

        uiChildren.sortByDescending { (it as EditGuiElement).priority }
        for (element in uiChildren) {
            element as EditGuiElement
            if (element.isDoubleWidth) {
                if (currentSide == 1) {
                    currentY += previousHeight
                    currentSide = 0
                }
                element.updatePosition(X_ALIGNMENT_LEFT, currentY)
                currentY += element.height
                continue
            }

            if (currentSide == 0) {
                element.updatePosition(X_ALIGNMENT_LEFT, currentY)
                currentSide = 1
                previousHeight = element.height
                continue
            }
            element.updatePosition(X_ALIGNMENT_RIGHT, currentY)
            currentY += element.height
            currentSide = 0
        }
        if (currentSide == 1) {
            currentY += previousHeight
        }
        this.height = currentY
    }

}