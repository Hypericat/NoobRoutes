package noobroutes.ui.clickgui

import net.minecraft.client.renderer.GlStateManager
import noobroutes.Core.mc
import noobroutes.font.MinecraftFont
import noobroutes.ui.clickgui.animations.impl.ColorAnimation
import noobroutes.ui.clickgui.elements.menu.ElementTextField
import noobroutes.ui.clickgui.util.ColorUtil
import noobroutes.ui.clickgui.util.ColorUtil.buttonColor
import noobroutes.ui.clickgui.util.ColorUtil.withAlpha
import noobroutes.ui.util.MouseUtils
import noobroutes.utils.render.*
import org.lwjgl.input.Keyboard

object SearchBar {

    var currentSearch = ""
    private var listening = false
    private val isHovered get() = MouseUtils.isAreaHovered(mc.displayWidth / 2f - 200f, mc.displayHeight - 100f, 400f, 30f)
    private val colorAnim = ColorAnimation(100)

    fun draw() {
        GlStateManager.pushMatrix()
        scale(1f / scaleFactor, 1f / scaleFactor, 1f)

        translate(mc.displayWidth / 2f, mc.displayHeight - 100f)
        roundedRectangle(-200f, 0f, 400f, 30f, ColorUtil.moduleButtonColor, 9f)
        if (listening || colorAnim.isAnimating()) {
            val color = colorAnim.get(ColorUtil.clickGUIColor, buttonColor, listening)
            rectangleOutline(-202f, -1f, 404f, 32f, color, 9f,3f)
        }
        if (currentSearch.isEmpty()) {
            text("Search here...", 0f, 18f, Color.WHITE.withAlpha(0.5f), 18f, MinecraftFont.REGULAR, TextAlign.Middle)
        } else text(currentSearch, 0f, 12f, Color.WHITE, 18f, MinecraftFont.REGULAR, TextAlign.Middle)
        translate(-mc.displayWidth / 4f, -mc.displayHeight / 4f + 200f)
        scale(scaleFactor, scaleFactor, 1f)
        GlStateManager.popMatrix()
    }

    fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton == 0 && isHovered) {
            if (colorAnim.start()) listening = !listening
            return true
        } else if (listening) {
            if (colorAnim.start()) listening = false
        }
        return false
    }

    fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (listening) {
            when (keyCode) {
                Keyboard.KEY_ESCAPE, Keyboard.KEY_NUMPADENTER, Keyboard.KEY_RETURN -> if (colorAnim.start()) listening = false
                Keyboard.KEY_BACK -> currentSearch = currentSearch.dropLast(1)
                !in ElementTextField.keyBlackList -> currentSearch += typedChar.toString()
            }
            if (currentSearch.length > "Auto-Renew Hollows Pass".length) currentSearch = currentSearch.dropLast(1)
            return true
        }
        return false
    }
}