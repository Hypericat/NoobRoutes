package noobroutes.ui.editgui.elements

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.ColorPalette
import noobroutes.ui.ColorPalette.TEXT_OFFSET
import noobroutes.ui.ColorPalette.buttonColor
import noobroutes.ui.ColorPalette.clickGUIColor
import noobroutes.ui.editgui.EditGuiBase
import noobroutes.ui.editgui.EditGuiElement
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.utils.render.Color
import noobroutes.utils.render.ColorUtil.darkerIf
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.popStencil
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.stencilRoundedRectangle
import noobroutes.utils.render.text

class EditGuiCheckBox(
    val name: String,
    val options: ArrayList<String>,
    val getter: () -> Array<Boolean>,
    val setter: (Array<Boolean>) -> Unit
) : UiElement(0f, 0f), EditGuiElement {
    override var priority: Int = 1
    override val isDoubleWidth: Boolean = true

    val optionsHeight = BUTTON_HEIGHT * options.size

    override val height: Float get() = optionsHeight + 15f + TEXT_HEIGHT

    private inline var elementValue
        get() = getter.invoke()
        set(value) = setter.invoke(value)

    companion object {
        //16 - text scale
        private const val BUTTON_HEIGHT = 40f
        private const val HALF_BUTTON_HEIGHT = BUTTON_HEIGHT * 0.5f
        private const val TEXT_HEIGHT = 30f
        private const val HALF_TEXT_HEIGHT = TEXT_HEIGHT * 0.5f
        private const val BUTTON_WIDTH = EditGuiBase.WIDTH - TEXT_OFFSET * 2f - 60f //60f is X_ALIGNMENT_LEFT * 2f
        private const val HALF_WIDTH = BUTTON_WIDTH * 0.5f
    }
    private val buttonAnimations = Array(options.size) { ColorAnimation(150) }
    private val buttonToggled = elementValue



    private fun isHovered(index: Int): Boolean {
        return isAreaHovered(0f, index * BUTTON_HEIGHT + TEXT_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT)
    }


    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton != 0) return false

        for (i in options.indices) {
            if (isHovered(i)) {

                if (!buttonAnimations[i].start()) return false

                buttonToggled[i] = !buttonToggled[i]
                elementValue = buttonToggled
                return true
            }
        }


        return false
    }


    override fun draw() {
        GlStateManager.pushMatrix()
        translate(x + TEXT_OFFSET, y)

        text(name,0f, HALF_TEXT_HEIGHT, ColorPalette.textColor, 16f)
        stencilRoundedRectangle(0f, TEXT_HEIGHT, BUTTON_WIDTH, optionsHeight, 10f)


        for (i in options.indices) {
            drawButton(options[i], TEXT_HEIGHT + BUTTON_HEIGHT * i, i)
        }

        popStencil()
        GlStateManager.popMatrix()
    }

    private fun drawButton(name: String, y: Float, index: Int) {
        roundedRectangle(0f, y, BUTTON_WIDTH, BUTTON_HEIGHT, ColorPalette.elementBackground)
        val color = buttonAnimations[index].get(clickGUIColor, Color.WHITE, buttonToggled[index])
            .darkerIf(isHovered(index), 0.7f)
        text(name, HALF_WIDTH, y + HALF_BUTTON_HEIGHT, color, 16f, align = TextAlign.Middle)
    }

}