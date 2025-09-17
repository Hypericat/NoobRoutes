package noobroutes.ui.editgui.elements

import noobroutes.ui.editgui.EditGuiElement
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.CubicBezierAnimation

class EditGuiDropdownSelector(
    val name: String,
    val options: ArrayList<String>,
    val initialValue: Int
) : UiElement(0f, 0f), EditGuiElement {
    override val priority: Int = 3
    override val isDoubleWidth: Boolean = true
    override val height: Float get() = getSelectorHeight()

    private val settingAnim = CubicBezierAnimation(200, 0.4, 0, 0.2, 1)


    var extended = false

    companion object {
        //16 - text scale
        private const val BUTTON_HEIGHT = 40f
        private const val TEXT_HEIGHT = 20f


        private const val EXTENDED_MAX_HEIGHT = TEXT_HEIGHT + BUTTON_HEIGHT * 5f
    }


    private fun getSelectorHeight(): Float{
        return BUTTON_HEIGHT + TEXT_HEIGHT
    }



}