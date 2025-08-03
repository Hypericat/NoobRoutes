package noobroutes.ui.test

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.Screen
import noobroutes.utils.render.Color
import noobroutes.utils.render.popStencil
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.stencil
import noobroutes.utils.render.stencilRoundedRectangle

object StencilTest : Screen() {
    override fun draw() {
        scaleUI()
        stencilRoundedRectangle(0f, 0f, 1000f, 1000f)
        stencilRoundedRectangle(650f, 650f, 350f, 350f, 0f, 0f, false)
        roundedRectangle(500f, 500f, 500f, 500f, Color.GREEN)
        popStencil()
        stencilRoundedRectangle(600f, 600f, 400f, 400f, 0f, 0f, true)
        roundedRectangle(500f, 500f, 500f, 500f, Color.BLUE)
        popStencil()
        stencilRoundedRectangle(550f, 550f, 450f, 450f, 0f, 0f, true)
        roundedRectangle(500f, 500f, 500f, 500f, Color.RED)
        popStencil()
        popStencil()

        GlStateManager.popMatrix()
    }
}