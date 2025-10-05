package noobroutes.ui.editgui

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.ColorPalette
import noobroutes.ui.editgui.elements.EditGuiSelector
import noobroutes.ui.editgui.elements.EditGuiSliderElement
import noobroutes.ui.editgui.elements.EditGuiSwitchElement
import noobroutes.ui.util.MouseUtils
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
        fun addSlider(name: String, min: Double, max: Double, increment: Double, roundTo: Int, getter: () -> Double, setter: (Double) -> Unit, priority: Int? = null) {
            val element = EditGuiSliderElement(name, min, max, increment, roundTo, getter, setter)
            priority?.let {
                element.priority = it
            }
            elements.add(
                element
            )
        }

        fun addSwitch(name: String, getter: () -> Boolean, setter: (Boolean) -> Unit, priority: Int? = null){
            val element = EditGuiSwitchElement(name, getter, setter)
            priority?.let {
                element.priority = it
            }
            elements.add(
                element
            )
        }
        fun addSelector(name: String, options: ArrayList<String>, getter: () -> Int, setter: (Int) -> Unit, priority: Int? = null) {
            val element = EditGuiSelector(name, options, getter, setter)
            priority?.let {
                element.priority = it
            }
            elements.add(
                element
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
            var currentY = 80f
            var currentSide = 0
            var previousHeight = 0f


            elements.sortByDescending { it.priority }
            for (element in elements) {
                element as UiElement
                if (element.isDoubleWidth) {
                    if (currentSide == 1) {
                        currentY += previousHeight
                        currentSide = 0
                    }
                    element.updatePosition(X_ALIGNMENT_LEFT, currentY)
                    base.addChild(element)
                    currentY += element.height
                    continue
                }

                if (currentSide == 0) {
                    element.updatePosition(X_ALIGNMENT_LEFT, currentY)
                    base.addChild(element)
                    currentSide = 1
                    previousHeight = element.height
                    continue
                }
                element.updatePosition(X_ALIGNMENT_RIGHT, currentY)
                base.addChild(element)
                currentY += element.height
                currentSide = 0
            }
            if (currentSide == 1) {
                currentY += previousHeight
            }
            base.name = name
            base.height = currentY
            base.onOpen = this.onOpen
            base.onClose = this.onClose
            base.updatePosition(editGuiBaseX, editGuiBaseY)
            return base
        }
    }

    var dragging = false
    var x2 = 0f
    var y2 = 0f
    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton != 0) return false
        if (isAreaHovered(0f, 0f, 600f, 70f)) {
            x2 = x - MouseUtils.mouseX
            y2 = y - MouseUtils.mouseY
            dragging = true
            return true
        }
        return false
    }

    override fun mouseReleased(): Boolean {
        dragging = false
        return false
    }

    fun updateYPositions(){
        var currentY = 80f
        var currentSide = 0
        var previousHeight = 0f
        val elements = uiChildren.toMutableList()

        elements.sortByDescending { (it as EditGuiElement).priority }
        for (element in elements) {
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

    override fun draw() {
        GlStateManager.pushMatrix()
        if (dragging) {
            updatePosition(x2 + MouseUtils.mouseX, y2 + MouseUtils.mouseY)
            editGuiBaseX = x
            editGuiBaseY = y
        }
        translate(x, y)
        blurRoundedRectangle(0f, 0f, 600f, height, 20f, 20f, 20f, 20f, 0.5f)
        roundedRectangle(0f, 0f, 600, 70, ColorPalette.titlePanelColor,  ColorPalette.titlePanelColor, Color.TRANSPARENT, 0, 20f, 20f, 0f, 0f, 0f)
        roundedRectangle(0f, 0f, 600, height, ColorPalette.buttonColor, radius = 20)
        text(name, X_ALIGNMENT_LEFT - 10, 37.5, Color.WHITE, size = 30)
        GlStateManager.popMatrix()
    }

    companion object {
        var editGuiBaseX = 0f
        var editGuiBaseY = 0f

        private const val X_ALIGNMENT_LEFT = 30f
        private const val X_ALIGNMENT_RIGHT = 300f
        const val WIDTH = 600f
    }
}