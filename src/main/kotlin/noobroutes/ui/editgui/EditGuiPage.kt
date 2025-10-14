package noobroutes.ui.editgui

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.editgui.EditGuiBase.Companion.X_ALIGNMENT_LEFT
import noobroutes.ui.editgui.EditGuiBase.Companion.X_ALIGNMENT_RIGHT
import noobroutes.ui.editgui.elements.EditGuiDescription
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
    private enum class Column { LEFT, RIGHT }

    fun updateYPositions() {
        var currentY = 80f
        var currentColumn = Column.LEFT
        var leftColumnHeight = 0f

        val descriptions = uiChildren
            .filterIsInstance<EditGuiDescription>()
            .filter { it.tiedElement != null }


        val boundDescriptions = mutableListOf<EditGuiDescription>()
        uiChildren.sortByDescending { (it as EditGuiElement).priority }

        for (element in uiChildren) {
            if (element in boundDescriptions) continue
            element as EditGuiElement
            when {
                element.isDoubleWidth -> {
                    if (currentColumn == Column.RIGHT) currentY += leftColumnHeight

                    element.updatePosition(X_ALIGNMENT_LEFT, currentY)
                    currentY += element.height
                    currentColumn = Column.LEFT
                }

                currentColumn == Column.LEFT -> {
                    element.updatePosition(X_ALIGNMENT_LEFT, currentY)
                    leftColumnHeight = element.height
                    currentColumn = Column.RIGHT
                }

                else -> {
                    element.updatePosition(X_ALIGNMENT_RIGHT, currentY)
                    currentY += maxOf(element.height, leftColumnHeight)
                    currentColumn = Column.LEFT
                }
            }

            val boundDescription = descriptions.firstOrNull { it.tiedElement == element }
            if (boundDescription != null) {
                if (currentColumn == Column.RIGHT) currentY += leftColumnHeight
                boundDescriptions.add(boundDescription)
                boundDescription.updatePosition(X_ALIGNMENT_LEFT, currentY)
                currentY += boundDescription.height
                currentColumn = Column.LEFT

            }
        }

        if (currentColumn == Column.RIGHT) {
            currentY += leftColumnHeight
        }

        this.height = currentY
    }


}