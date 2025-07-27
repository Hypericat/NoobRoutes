package noobroutes.ui.clickgui.elements.menu

import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.font.Font
import noobroutes.ui.ColorPalette
import noobroutes.ui.ColorPalette.clickGUIColor
import noobroutes.ui.ColorPalette.elementBackground
import noobroutes.ui.ColorPalette.textColor
import noobroutes.ui.clickgui.ClickGUI.TEXTOFFSET
import noobroutes.ui.clickgui.elements.Element
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.ui.clickgui.elements.ModuleButton
import noobroutes.ui.clickgui.util.HoverHandler
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.utils.render.ColorUtil.brighter
import noobroutes.utils.render.*
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

/**
 * Renders all the modules.
 *
 * Backend made by Aton, with some changes
 * Design mostly made by Stivais
 *
 * @author Stivais, Aton
 * @see [Element]
 */
class ElementKeyBind(parent: ModuleButton, setting: KeybindSetting) :
    Element<KeybindSetting>(parent, setting, ElementType.KEY_BIND) {

    private val colorAnim = ColorAnimation(100)

    private val hover = HoverHandler(0, 150)

    private val buttonColor: Color
        inline get() = ColorPalette.buttonColor.brighter(1 + hover.percent() / 500f)

    override fun draw() {
        val key = setting.value.key
        val value = if (key > 0) Keyboard.getKeyName(key) ?: "Err"
        else if (key < 0) Mouse.getButtonName(key + 100)
        else "None"

        roundedRectangle(x, y, w, h, elementBackground)

        val width = getTextWidth(value, 12f)
        hover.handle(x + w - 20 - width, y + 4, width + 12f, 22f)

        roundedRectangle(x + w - 20 - width, y + 4, width + 12f, 22f, buttonColor, 5f)

        if (listening || colorAnim.isAnimating()) {
            val color = colorAnim.get(clickGUIColor, buttonColor, listening)
            rectangleOutline(x + w - 21 - width, y + 3, width + 12.5f, 22.5f, color, 4f,1.5f)
        }

        text(name,  x + TEXTOFFSET, y + h / 2, textColor, 12f, Font.REGULAR)
        text(value, x + w - 14, y + 8f, textColor, 12f, Font.REGULAR, TextAlign.Right, TextPos.Top)
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton == 0 && isHovered) {
            if (colorAnim.start()) listening = !listening
            return true
        } else if (listening && colorAnim.start()) {
            listening = false
        }
        return false
    }

    override fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (listening) {
            if (keyCode == Keyboard.KEY_ESCAPE || keyCode == Keyboard.KEY_BACK) {
                setting.value.key = Keyboard.KEY_NONE
                if (colorAnim.start()) listening = false
            } else if (keyCode == Keyboard.KEY_NUMPADENTER || keyCode == Keyboard.KEY_RETURN) {
                if (colorAnim.start()) listening = false
            } else {
                setting.value.key = keyCode
                if (colorAnim.start()) listening = false
            }
            return true
        }
        return false
    }
}