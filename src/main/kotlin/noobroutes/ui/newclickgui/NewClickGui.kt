package noobroutes.ui.newclickgui

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.Screen
import noobroutes.ui.newclickgui.elements.ClickGUIBase
import noobroutes.ui.util.shader.GaussianBlurShader
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import kotlin.math.sign

object NewClickGui : Screen() {
    val base = ClickGUIBase()
    override fun draw() {
        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        scaleUI()
        GaussianBlurShader.captureBackground()
        base.handleDraw()
        GaussianBlurShader.cleanup()
        resetScale()
        GlStateManager.popMatrix()
    }

    override fun onScroll(amount: Int) {
        if (Mouse.getEventDWheel() == 0) return
        base.handleScroll(amount.sign * 16)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        base.handleMouseClicked(mouseButton)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        base.handleKeyTyped(typedChar, keyCode)
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        if (state != 0) return
        base.handleMouseReleased()
    }

}