package noobroutes.ui.newclickgui

import noobroutes.ui.Screen
import noobroutes.ui.util.elements.SwitchElement

object ElementTestGUI : Screen() {
    private val testSwitches = listOf(
        SwitchElement(
            "Test",
            false,
            300f,
            300f,
            400f,
            100f
        ),
        SwitchElement(
            "Test1",
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
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (mouseButton != 0) return
        testSwitches.forEach { it.mouseLeftClicked() }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        super.keyTyped(typedChar, keyCode)
    }



}