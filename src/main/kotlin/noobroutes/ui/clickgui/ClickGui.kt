package noobroutes.ui.clickgui

import net.minecraft.client.renderer.GlStateManager
import noobroutes.config.Config
import noobroutes.features.render.ClickGUIModule
import noobroutes.ui.Screen
import noobroutes.ui.clickgui.elements.ClickGUIBase
import noobroutes.ui.clickgui.elements.SearchBar
import noobroutes.ui.util.shader.GaussianBlurShader
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import kotlin.math.sign

object ClickGui : Screen() {
    override fun draw() {
        scaleUI()
        GlStateManager.enableBlend()
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        if (ClickGUIModule.blur) GaussianBlurShader.captureBackground()
        ClickGUIBase.doHandleDraw()
        if (ClickGUIModule.blur) GaussianBlurShader.cleanup()
        resetScale()
    }

    override fun onScroll(amount: Int) {
        if (Mouse.getEventDWheel() == 0) return
        ClickGUIBase.handleScroll(amount.sign * 16)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        ClickGUIBase.handleMouseClicked(mouseButton)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (ClickGUIBase.doHandleKeyTyped(typedChar, keyCode)) return
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        if (state != 0) return
        ClickGUIBase.handleMouseReleased()
    }
    override fun initGui() {
        ClickGUIBase.onGuiInit()
    }

    override fun onGuiClosed() {
        SearchBar.onGuiClosed()
        Config.save()
    }

}