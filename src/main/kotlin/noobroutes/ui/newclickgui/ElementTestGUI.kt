package noobroutes.ui.newclickgui

import noobroutes.features.settings.impl.Keybinding
import noobroutes.ui.ColorPalette
import noobroutes.ui.Screen
import noobroutes.ui.util.ElementRenderer.TEXT_OFFSET
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.elements.KeybindElement
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
        KeybindElement("Test 1", Keybinding(Keyboard.KEY_NONE), 300f, 500f, 400f, 100f)


    )

    override fun draw() {
        scaleUI()
        testElements.forEach { it.draw() }
        resetScale()
        text("Unscaled Text", 100f, 100f, ColorPalette.text, 8f, fontType = ColorPalette.font)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton != 0) return
        testElements.forEach { it.mouseClicked(mouseButton) }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        testElements.forEach { it.keyTyped(typedChar, keyCode) }
        super.keyTyped(typedChar, keyCode)
    }


}