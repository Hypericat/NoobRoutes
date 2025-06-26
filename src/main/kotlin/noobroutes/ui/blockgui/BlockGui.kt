package noobroutes.ui.blockgui

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.MathHelper
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Vec3
import noobroutes.Core
import noobroutes.config.Config
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.Ring
import noobroutes.features.floor7.autop3.rings.*
import noobroutes.features.render.ClickGUIModule
import noobroutes.ui.Screen
import noobroutes.ui.blockgui.blockselector.BlockSelector
import noobroutes.ui.blockgui.blockselector.BlockSelector.scrollOffset
import noobroutes.ui.clickgui.util.ColorUtil
import noobroutes.ui.clickgui.util.ColorUtil.darkerIf
import noobroutes.ui.editUI.elements.ElementCheckBox
import noobroutes.ui.editUI.elements.ElementSlider
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.MouseUtils.isAreaHovered
import noobroutes.ui.util.MouseUtils.mouseX
import noobroutes.ui.util.MouseUtils.mouseY
import noobroutes.utils.render.*
import kotlin.math.floor
import kotlin.math.sign


object BlockGui : Screen() {
    val isResetHovered get() = isAreaHovered(mc.displayWidth * 0.5f - 75f, mc.displayHeight * 0.9f - 40f, 150f, 80f)

    override fun onScroll(amount: Int) {
        val actualAmount = amount.sign * 16
        scrollOffset += actualAmount
    }

    override fun initGui() {
        if (OpenGlHelper.shadersSupported && mc.renderViewEntity is EntityPlayer && ClickGUIModule.blur) {
            mc.entityRenderer.stopUseShader()
            mc.entityRenderer.loadShader(ResourceLocation("shaders/post/blur.json"))
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {

        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        BlockEditor.dragging = false
        BlockSelector.dragging = false
    }


    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (isResetHovered) {
            BlockSelector.originX = 100f
            BlockSelector.originY = 200f
            BlockEditor.originX = 500f
            BlockEditor.originY = 200f
        }
        if (BlockEditor.mouseClicked()) return
        BlockSelector.mouseClicked()
    }

    override fun onGuiClosed() {
        mc.entityRenderer.stopUseShader()
        AutoP3.saveRings()
        Config.save()
    }

    override fun draw() {
        GlStateManager.pushMatrix()
        translate(0f, 0f, 200f)
        scale(1f / scaleFactor, 1f / scaleFactor, 1f)
        BlockSelector.draw()
        BlockEditor.draw()

        roundedRectangle(mc.displayWidth * 0.5 - 75, mc.displayHeight * 0.9f - 40, 150f, 80f, ColorUtil.buttonColor, 15f)
        text("Reset", mc.displayWidth * 0.5, mc.displayHeight * 0.9f, Color.WHITE.darkerIf(isResetHovered), 26f, align = TextAlign.Middle)

        scale(scaleFactor, scaleFactor, 1f)
        GlStateManager.popMatrix()
    }


}