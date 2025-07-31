package noobroutes.ui.blockgui

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.OpenGlHelper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation
import noobroutes.config.Config
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.render.ClickGUIModule
import noobroutes.ui.Screen
import noobroutes.ui.blockgui.blockselector.BlockSelector
import noobroutes.ui.clickgui.util.ColorUtil
import noobroutes.ui.clickgui.util.ColorUtil.darkerIf
import noobroutes.ui.util.MouseUtils.isAreaHovered
import noobroutes.utils.render.*


object BlockGui : Screen() {
    private val isResetHovered get() = isAreaHovered(mc.displayWidth * 0.5f - 75f, mc.displayHeight * 0.9f - 40f, 150f, 80f)

    override fun onScroll(amount: Int) {
        BlockSelector.onScroll(amount)
    }

    override fun initGui() {
        if (OpenGlHelper.shadersSupported && mc.renderViewEntity is EntityPlayer && ClickGUIModule.blur) {
            mc.entityRenderer.stopUseShader()
            mc.entityRenderer.loadShader(ResourceLocation("shaders/post/blur.json"))
        }
    }



    override fun keyTyped(typedChar: Char, keyCode: Int) {
        //BlockEditor.keyTyped(typedChar, keyCode)
        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseReleased(mouseX: Int, mouseY: Int, state: Int) {
        //BlockEditor.mouseReleased()
        BlockSelector.dragging = false
    }


    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (isResetHovered) {
            BlockSelector.originX = 100f
            BlockSelector.originY = 200f
            //BlockEditor.originX = 500f
            //BlockEditor.originY = 200f
        }
        //if (BlockEditor.mouseClicked(mouseButton)) return
        BlockSelector.mouseClicked()
    }

    override fun onGuiClosed() {
        mc.entityRenderer.stopUseShader()
        Config.save()
    }

    override fun draw() {
        GlStateManager.pushMatrix()
        translate(0f, 0f, 200f)
        scale(1f / scaleFactor, 1f / scaleFactor, 1f)
        BlockSelector.draw()
        //BlockEditor.draw()

        roundedRectangle(mc.displayWidth * 0.5 - 75, mc.displayHeight * 0.9f - 40, 150f, 80f, ColorUtil.buttonColor, 15f)
        text("Reset", mc.displayWidth * 0.5, mc.displayHeight * 0.9f, Color.WHITE.darkerIf(isResetHovered), 26f, align = TextAlign.Middle)

        scale(scaleFactor, scaleFactor, 1f)
        GlStateManager.popMatrix()
    }


}