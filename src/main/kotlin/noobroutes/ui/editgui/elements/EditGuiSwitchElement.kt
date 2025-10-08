package noobroutes.ui.editgui.elements

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.ColorPalette
import noobroutes.ui.clickgui.elements.SettingElement.Companion.BORDER_OFFSET
import noobroutes.ui.editgui.EditGuiBase
import noobroutes.ui.editgui.EditGuiElement
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.elements.SwitchElement
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.text

class EditGuiSwitchElement(
    val name: String, val getter: () -> Boolean, val setter: (Boolean) -> Unit
) : UiElement(0f, 0f), EditGuiElement {
    override var priority: Int = 2
    override val isDoubleWidth: Boolean = false
    override val height: Float = 55f

    //10f

    inline var value
        get() = getter.invoke()
        set(value) = setter(value)
//height * 0.5f -
    val switch = SwitchElement(1.6f, value, 0f, SwitchElement.SWITCH_HEIGHT)

    init {
        switch.addValueChangeListener {
           value = it
        }
        addChild(switch)
    }

    override fun draw() {
        GlStateManager.pushMatrix()
        translate(x + BORDER_OFFSET, y)

        switch.x = SwitchElement.SWITCH_WIDTH + 120f
        text(name, 0f, SwitchElement.SWITCH_HEIGHT, ColorPalette.textColor, 16f)
        GlStateManager.popMatrix()
    }

}