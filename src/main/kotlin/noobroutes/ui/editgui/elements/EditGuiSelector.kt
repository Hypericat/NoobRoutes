package noobroutes.ui.editgui.elements

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.ColorPalette
import noobroutes.ui.ColorPalette.TEXT_OFFSET
import noobroutes.ui.ColorPalette.buttonColor
import noobroutes.ui.editgui.EditGui
import noobroutes.ui.editgui.EditGuiBase
import noobroutes.ui.editgui.EditGuiBase.Companion.BUTTON_WIDTH
import noobroutes.ui.editgui.EditGuiBase.Companion.HALF_BUTTON_WIDTH
import noobroutes.ui.editgui.EditGuiElement
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.CubicBezierAnimation
import noobroutes.ui.util.animations.impl.LinearAnimation
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.popStencil
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.stencilRoundedRectangle
import noobroutes.utils.render.text
import noobroutes.utils.round

class EditGuiSelector(
    val name: String,
    val options: ArrayList<String>,
    val getter: () -> Int,
    val setter: (Int) -> Unit
) : UiElement(0f, 0f), EditGuiElement {
    override var priority: Int = 1
    override val isDoubleWidth: Boolean = true
    override val height: Float get() = getSelectorHeight() + 15f + TEXT_HEIGHT

    private val settingAnim = CubicBezierAnimation(200, 0.4, 0, 0.2, 1)
    private val scrollAnim = LinearAnimation<Float>(200L)


    private inline var elementValue
        get() = getter.invoke()
        set(value) = setter.invoke(value)

    var extended = false

    companion object {
        //16 - text scale
        private const val BUTTON_HEIGHT = 40f
        private const val HALF_BUTTON_HEIGHT = BUTTON_HEIGHT * 0.5f
        private const val TEXT_HEIGHT = 30f
        private const val HALF_TEXT_HEIGHT = TEXT_HEIGHT * 0.5f
        private const val HEIGHT_START = BUTTON_HEIGHT
        private const val OPTION_LIST_START = TEXT_HEIGHT + BUTTON_HEIGHT
        private const val MAX_VISIBLE_OPTIONS = 4


        private const val SCROLL_BAR_WIDTH = 3f
    }

    private val scrollBarHeight = run {
        val totalUnselectedOptions = options.size - 1
        if (totalUnselectedOptions <= MAX_VISIBLE_OPTIONS) {
            BUTTON_HEIGHT * MAX_VISIBLE_OPTIONS.toFloat()
        } else {
            (BUTTON_HEIGHT * MAX_VISIBLE_OPTIONS * MAX_VISIBLE_OPTIONS / totalUnselectedOptions).coerceAtLeast(20f)
        }
    }

    private val maxHeight = HEIGHT_START + BUTTON_HEIGHT * (options.size - 1).coerceAtMost(4)

    private fun getSelectorHeight(): Float{
        return settingAnim.get(HEIGHT_START, maxHeight, !extended)
    }

    private inline val hoveredSelectedOption
        get() = isAreaHovered(
            0f, TEXT_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT
        )

    private fun mouseWithinOptionWindow(): Boolean {
        return isAreaHovered(0f, TEXT_HEIGHT, BUTTON_WIDTH, maxHeight)
    }

    override fun onScroll(amount: Int): Boolean {
        if (extended && mouseWithinOptionWindow()) {
            scrollTarget = (scrollTarget + amount).coerceIn(-BUTTON_HEIGHT * (options.size - 5).coerceAtLeast(0), 0f)
            scrollAnim.start(true)
            return true
        }
        return false
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton != 0) return false
        if (!extended && hoveredSelectedOption && settingAnim.start()) {
            extended = true
            return true
        }
        if (!extended || !mouseWithinOptionWindow()) return false

        val sortedOptions = options.toMutableList()
        sortedOptions.removeAt(elementValue)
        sortedOptions.add(0, options[elementValue])
        for (i in sortedOptions.indices) {
            if (isAreaHovered(0f, scrollOffset + (i + 1) * BUTTON_HEIGHT, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                if (!settingAnim.start()) return false
                elementValue = options.indexOf(sortedOptions[i])
                settingAnim.start(true)
                unExtend()
                return true
            }
        }

        if (settingAnim.start()) {
            unExtend()
        }

        return false
    }

    private fun unExtend(){
        scrollTarget = 0f
        scrollAnim.start(true)
        extended = false
    }

    private var scrollOffset = 0f
    private var scrollTarget = 0f

    override fun draw() {
        GlStateManager.pushMatrix()
        translate(x + TEXT_OFFSET, y)


        scrollOffset = scrollAnim.get(scrollOffset, scrollTarget).round(0).toFloat()


        text(name,0f, HALF_TEXT_HEIGHT, ColorPalette.textColor, 16f)
        stencilRoundedRectangle(0f, TEXT_HEIGHT, BUTTON_WIDTH, getSelectorHeight(), 10f)
        drawButton(options[elementValue], TEXT_HEIGHT + scrollOffset)

        if (!extended && !settingAnim.isAnimating()) {
            popStencil()
            GlStateManager.popMatrix()
            return
        }
        EditGui.updateBaseY()

        val unselectedOptionsList = options.toMutableList()
        unselectedOptionsList.removeAt(elementValue)

        for (i in unselectedOptionsList.indices) {
            drawButton(unselectedOptionsList[i], OPTION_LIST_START + BUTTON_HEIGHT * i + scrollOffset)
        }
        if (options.size > 5) {
            val scrollBarY = TEXT_HEIGHT +
                    (-scrollOffset / (BUTTON_HEIGHT * (options.size - 5))) * (getSelectorHeight() - scrollBarHeight)

            roundedRectangle(
                BUTTON_WIDTH - SCROLL_BAR_WIDTH,
                scrollBarY,
                SCROLL_BAR_WIDTH,
                scrollBarHeight,
                ColorPalette.textColor
            )
        }
        popStencil()
        GlStateManager.popMatrix()
    }

    private fun drawButton(name: String, y: Float){
        roundedRectangle(0f, y, BUTTON_WIDTH, BUTTON_HEIGHT, buttonColor)
        text(name, HALF_BUTTON_WIDTH, y + HALF_BUTTON_HEIGHT, ColorPalette.textColor, 16f, align = TextAlign.Middle)
    }

}