package noobroutes.ui.test

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.Screen
import noobroutes.ui.util.elements.colorelement.ColorPopoutElement
import noobroutes.utils.render.Color
import noobroutes.utils.render.gapOutline
import noobroutes.utils.render.popStencil
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.stencil
import noobroutes.utils.render.stencilRoundedRectangle

object StencilTest : Screen() {

    val colorPopoutElement = ColorPopoutElement(400f, 400f, true, Color.RED)

    override fun draw() {
        scaleUI()
        GlStateManager.translate(200f, 0f, 0f)
        colorPopoutElement.doHandleDraw()

        GlStateManager.popMatrix()
    }
}