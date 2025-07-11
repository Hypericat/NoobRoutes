package noobroutes.ui.newclickgui

import noobroutes.features.settings.impl.Keybinding
import noobroutes.ui.ColorPalette
import noobroutes.ui.Screen
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.elements.KeybindElement
import noobroutes.ui.util.elements.SliderElement
import noobroutes.ui.util.elements.SwitchElement
import noobroutes.utils.render.text
import org.lwjgl.input.Keyboard

object ElementTestGUI : Screen() {
    private val testElements = listOf<UiElement>(
        SwitchElement(
            "Test",
            24f,
            false,
            300f,
            300f,
            400f,
            100f
        ),
        KeybindElement("Test 1", Keybinding(Keyboard.KEY_NONE), 300f, 500f, 400f, 100f),
        SliderElement(
            "Test 2",
            300f,
            600f,
            400f,
            100f,
            20.0,
            20.0,
            80.0,
            1.0,
            "gph"
        )
    )

    override fun draw() {
        scaleUI()
        testElements.forEach { it.draw() }
        resetScale()
        text("Unscaled Text", 100f, 100f, ColorPalette.text, 8f, fontType = ColorPalette.font)
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