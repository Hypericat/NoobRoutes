package noobroutes.ui.newclickgui

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.Screen
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.elements.colorelement.ColorElement
import noobroutes.utils.render.Color
import org.lwjgl.opengl.GL11

object ElementTestGUI : Screen() {
    private val testElements = listOf<UiElement>(
        ColorElement(100f, 300f, 100f, 35f, 8f, Color(50, 50, 50, 0.4f), true)
    )

    override fun draw() {
        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA)
        scaleUI()
        testElements.forEach { it.handleDraw() }
        resetScale()
        GlStateManager.popMatrix()
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        testElements.forEach { it.handleMouseClicked(mouseButton) }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (testElements.any { it.handleKeyTyped(typedChar, keyCode) }) return
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        if (state != 0) return
        testElements.forEach { it.handleMouseReleased() }
    }

}