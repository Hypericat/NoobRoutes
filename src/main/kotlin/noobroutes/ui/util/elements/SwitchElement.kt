package noobroutes.ui.util.elements

import noobroutes.font.Font
import noobroutes.ui.ColorPalette
import noobroutes.ui.util.animations.impl.LinearAnimation
import noobroutes.ui.util.ElementRenderer
import noobroutes.ui.util.ElementRenderer.TEXT_OFFSET
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.utils.render.text

class SwitchElement(
    name: String,
    val scale: Float,
    initialValue: Boolean,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    ) : UiElement(name, x, y, w, h), ElementValue<Boolean>  {
    override val elementValueChangeListeners = mutableListOf<(Boolean) -> Unit>()

    override var elementValue: Boolean = initialValue

    private val colorAnimation = ColorAnimation(250)
    private val linearAnimation = LinearAnimation<Float>(200)


    private inline val isHovered get() = ElementRenderer.isHoveredSwitch(
        x + w * 0.9f,
        y + halfHeight,
        1f,
        1f,
    )

    override fun draw() {
        drawName()
        ElementRenderer.drawSwitch(
            x + w * 0.9f,
            y + halfHeight,
            1.3f,
            1.3f,
            elementValue,
            linearAnimation,
            colorAnimation
        )
    }

    override fun mouseClicked(mouseButton: Int) {
        if (!isHovered || mouseButton != 0) return
        if (colorAnimation.start()) {
            linearAnimation.start()
            setValue(!elementValue)
        }
    }





}