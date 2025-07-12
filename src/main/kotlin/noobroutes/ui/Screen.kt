package noobroutes.ui


import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.renderer.GlStateManager
import noobroutes.utils.render.initUIFramebufferStencil
import noobroutes.utils.render.scale
import noobroutes.utils.render.scaleFactor
import noobroutes.utils.render.translate
import org.lwjgl.input.Mouse


abstract class Screen : GuiScreen() {


    protected fun scaleUI(){
        GlStateManager.pushMatrix()
        translate(0f, 0f, 200f)
        scale(1f / scaleFactor, 1f / scaleFactor, 1f)
    }
    protected fun resetScale(){
        scale(scaleFactor, scaleFactor, 1f)
        GlStateManager.popMatrix()
    }

    abstract fun draw()

    open fun onScroll(amount: Int) {}

    final override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        draw()
    }

    final override fun handleMouseInput() {
        super.handleMouseInput()
        val scrollEvent = Mouse.getEventDWheel()
        if (scrollEvent != 0) {
            onScroll(scrollEvent)
        }
    }

    final override fun doesGuiPauseGame(): Boolean {
        return false
    }
}