package noobroutes.ui.editgui

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.ColorPalette
import noobroutes.ui.editgui.elements.EditGuiSliderElement
import noobroutes.ui.editgui.elements.EditGuiSwitchElement
import noobroutes.ui.util.UiElement
import noobroutes.utils.render.Color
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.text

class EditGuiBase() : UiElement(0f, 0f) {
    var height = 0f
    var name = ""
    var onOpen: () -> Unit = {}
    var onClose: () -> Unit = {}


    class EditGuiBaseBuilder(){
        val elements = mutableListOf<EditGuiElement>()
        fun addSlider(name: String, min: Double, max: Double, increment: Double, roundTo: Int, getter: () -> Double, setter: (Double) -> Unit){
            elements.add(
                EditGuiSliderElement(name, min, max, increment, roundTo, getter, setter)
            )
        }

        fun addSwitch(name: String, getter: () -> Boolean, setter: (Boolean) -> Unit){
            elements.add(
                EditGuiSwitchElement(name, getter, setter)
            )
        }
        private var onOpen: () -> Unit = {}
        private var onClose: () -> Unit = {}
        private var name = ""
        fun setName(name: String) {
            this.name = name
        }
        fun setOnOpen(action: () -> Unit) {
            this.onOpen = action
        }
        fun setOnClose(action: () -> Unit) {
            this.onClose = action
        }

        fun build(): EditGuiBase {
            val base = EditGuiBase()
            var currentY = 105f
            var currentSide = 0

            elements.sortByDescending { it.priority }
            for (element in elements) {
                element as UiElement
                if (element.isDoubleWidth) {
                    element.updatePosition(X_ALIGNMENT_LEFT, currentY)
                    base.addChild(element)
                    currentY += element.height
                    continue
                }

                if (currentSide == 0) {
                    element.updatePosition(X_ALIGNMENT_LEFT, currentY)
                    base.addChild(element)
                    currentSide = 1
                    continue
                }
                element.updatePosition(X_ALIGNMENT_RIGHT, currentY)
                base.addChild(element)
                currentY += element.height
                currentSide = 0
            }
            base.name = name
            base.height = currentY + 75f
            base.onOpen = this.onOpen
            base.onClose = this.onClose
            return base
        }
    }


    override fun draw() {
        GlStateManager.pushMatrix()
        blurRoundedRectangle(x, y, 600f, height, 20f, 20f, 20f, 20f, 0.5f)
        translate(x, y)
        roundedRectangle(0f, 0f, 600, 70, ColorPalette.titlePanelColor,  ColorPalette.titlePanelColor, Color.TRANSPARENT, 0, 20f, 20f, 0f, 0f, 0f)
        roundedRectangle(0f, 0f, 600, height, ColorPalette.buttonColor, radius = 20)
        text(name, X_ALIGNMENT_LEFT - 10, 37.5, Color.WHITE, size = 30)
        GlStateManager.popMatrix()
    }

    companion object {
        private const val DEFAULT_X = 100f
        private const val DEFAULT_Y = 200f

        private const val X_ALIGNMENT_LEFT = 30f
        private const val X_ALIGNMENT_RIGHT = 300f
        const val WIDTH = 600f
    }
}