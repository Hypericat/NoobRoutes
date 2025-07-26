package noobroutes.ui.newclickgui.elements

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.elements.SwitchElement
import noobroutes.ui.util.elements.colorelement.ColorElement
import noobroutes.utils.render.Color

class ScaleTestElement(x: Float, y: Float) : UiElement(x, y) {

    init {
        addChild(
            ColorElement(50f, 30f, 100f, 35f, 8f, Color(50, 50, 50, 0.4f), true)
        )
    }

    override fun draw() {
        GlStateManager.pushMatrix()
        translate(x, y)
        scale(1.5f, 1.5f)
        GlStateManager.popMatrix()
    }
}