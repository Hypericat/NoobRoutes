package noobroutes.ui.newclickgui

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.ColorPalette
import noobroutes.ui.Screen
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.elements.TextBoxElement
import noobroutes.ui.util.elements.colorelement.ColorElement
import noobroutes.utils.render.Color
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.text
import org.lwjgl.opengl.GL11

object ElementTestGUI : Screen() {
    private val testElements = listOf<UiElement>(
        ColorElement(
            100f,
            500f,
            30f,
            15f,
            5f,
            Color.GREEN,
            false
        ),
        ColorElement(
            300f,
            500f,
            30f,
            15f,
        5f,
        Color.GREEN,
        true
    )
    )

    override fun draw() {
        GlStateManager.pushMatrix()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA)
        scaleUI()
        testElements.forEach { it.draw() }
        resetScale()
        GlStateManager.popMatrix()
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        testElements.forEach { it.mouseClicked(mouseButton) }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (testElements.any { it.keyTyped(typedChar, keyCode) }) return
        super.keyTyped(typedChar, keyCode)
    }
    private fun handleText(number: String):String {
        if (number.isNotEmpty() && number.last() == '.' && number.count { it == '.' } >= 2) {
            return number.dropLast(1)
        }
        return number
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        testElements.forEach { it.mouseReleased() }

    }

}