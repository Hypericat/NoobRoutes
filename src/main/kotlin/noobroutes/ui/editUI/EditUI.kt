package noobroutes.ui.editUI

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import noobroutes.Core
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.render.ClickGUIModule
import noobroutes.ui.Screen
import noobroutes.ui.clickgui.util.ColorUtil
import noobroutes.ui.editUI.elements.ElementSlider
import noobroutes.utils.render.Color
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.scale
import noobroutes.utils.render.scaleFactor
import noobroutes.utils.render.text
import noobroutes.utils.render.translate

object EditUI : Screen() {

    var ring: Ring? = null
    const val ORIGIN_X = 100f
    const val ORIGIN_Y = 100f
    const val X_ALIGNMENT_LEFT = ORIGIN_X + 30f

    fun openUI(ring: Ring) {
        this.ring = ring
        elements.add(
            ElementSlider(
                "Yaw",
                min = 0.0, max = 360.0,
                unit = "°",
                increment = 0.1,
                {ring.yaw.toDouble()}, {ring.yaw = it.toFloat()},
                X_ALIGNMENT_LEFT, ORIGIN_Y + 105,
                500f,
                104f
                )
        )
        Core.display = EditUI
    }

    val elements = mutableListOf<Element<*>>()


    override fun initGui() {
        if (OpenGlHelper.shadersSupported && mc.renderViewEntity is EntityPlayer && ClickGUIModule.blur) {
            mc.entityRenderer.stopUseShader()
            mc.entityRenderer.loadShader(ResourceLocation("shaders/post/blur.json"))
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        elements.forEach { it.keyTyped(typedChar, keyCode) }
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        elements.forEach { it.mouseReleased() }
    }

    override fun onGuiClosed() {
        mc.entityRenderer.stopUseShader()
        ring = null
        elements.clear()
    }

    override fun draw() {
        val ring = this.ring ?: return
        GlStateManager.pushMatrix()
        translate(0f, 0f, 200f)
        scale(1f / scaleFactor, 1f / scaleFactor, 1f)
        roundedRectangle(ORIGIN_X, ORIGIN_Y, 600, 800, ColorUtil.buttonColor, radius = 20)
        text(ring.type, X_ALIGNMENT_LEFT, ORIGIN_Y + 50, Color.WHITE, size = 38)


        scale(scaleFactor, scaleFactor, 1f)
        GlStateManager.popMatrix()
    }


}