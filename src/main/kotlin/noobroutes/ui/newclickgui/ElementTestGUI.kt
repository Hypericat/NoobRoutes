package noobroutes.ui.newclickgui

import noobroutes.ui.ColorPalette
import noobroutes.ui.Screen
import noobroutes.ui.util.ElementRenderer.TEXT_OFFSET
import noobroutes.ui.util.elements.SwitchElement
import noobroutes.utils.render.text

object ElementTestGUI : Screen() {
    private val testSwitches = listOf(
        SwitchElement(
            "Test",
            24f,
            false,
            300f,
            300f,
            400f,
            100f
        ),
        SwitchElement(
            "Test1",
            24f,
            false,
            300f,
            500f,
            300f,
            75f
        )


    )

    override fun draw() {
        scaleUI()
        testSwitches.forEach { it.draw() }
        resetScale()
        text("Unscaled Text", 100f, 100f, ColorPalette.text, 8f, fontType = ColorPalette.font)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton != 0) return
        testSwitches.forEach { it.mouseLeftClicked() }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        super.keyTyped(typedChar, keyCode)
    }

}