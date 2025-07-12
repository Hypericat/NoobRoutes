package noobroutes.ui.util.elements

import com.sun.org.apache.xpath.internal.operations.Bool
import net.minecraft.client.renderer.GlStateManager
import noobroutes.font.Font
import noobroutes.ui.ColorPalette
import noobroutes.ui.clickgui.util.ColorUtil.darker
import noobroutes.ui.clickgui.util.ColorUtil.darkerIf
import noobroutes.ui.util.ElementValue
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.Animation
import noobroutes.ui.util.animations.impl.EaseInOut
import noobroutes.utils.render.Color
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.text



class DualElement(
    name: String,
    val left: String,
    val right: String,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    override var elementValue: Boolean
) : UiElement(name, x, y, w, h), ElementValue<Boolean> {
    private val posAnim = EaseInOut(250)

    private val elementDrawLocation = w - DUAL_ELEMENT_HALF_WIDTH - ColorPalette.TEXT_OFFSET

    private inline val isRightHovered: Boolean
        get() = MouseUtils.isAreaHovered(
            x + elementDrawLocation,
            y + h * 0.5f - DUAL_ELEMENT_HALF_HEIGHT,
            DUAL_ELEMENT_HALF_WIDTH,
            DUAL_ELEMENT_HEIGHT
        )

    private inline val isLeftHovered: Boolean
        get() = MouseUtils.isAreaHovered(
            x + elementDrawLocation - DUAL_ELEMENT_HALF_WIDTH,
            y + h * 0.5f - DUAL_ELEMENT_HALF_HEIGHT,
            DUAL_ELEMENT_HALF_WIDTH,
            DUAL_ELEMENT_HEIGHT
        )


    companion object {
        const val DUAL_ELEMENT_WIDTH = 240f
        const val DUAL_ELEMENT_HEIGHT = 28f
        const val DUAL_ELEMENT_HALF_WIDTH = DUAL_ELEMENT_WIDTH * 0.5f
        const val DUAL_ELEMENT_HALF_HEIGHT = DUAL_ELEMENT_HEIGHT * 0.5f
        const val DUAL_LEFT_TEXT_POSITION = (DUAL_ELEMENT_WIDTH * 0.25) - DUAL_ELEMENT_HALF_WIDTH
        const val DUAL_RIGHT_TEXT_POSITION = (DUAL_ELEMENT_WIDTH * 0.75) - DUAL_ELEMENT_HALF_WIDTH


        fun drawDualElement(left: String, right: String, leftIsHovered: Boolean, rightIsHovered: Boolean, enabled: Boolean, x: Float, y: Float, xScale: Float, yScale: Float, posAnim: Animation<Float>) {
            GlStateManager.pushMatrix()
            GlStateManager.translate(x, y, 1f)
            GlStateManager.scale(xScale, yScale, 1f)
            roundedRectangle(
                -DUAL_ELEMENT_HALF_WIDTH,
                -DUAL_ELEMENT_HALF_HEIGHT,
                DUAL_ELEMENT_WIDTH,
                DUAL_ELEMENT_HEIGHT,
                ColorPalette.backgroundSecondary,
                radius = 5f
            )
            val pos = posAnim.get(0f, DUAL_ELEMENT_HALF_WIDTH, !enabled)
            roundedRectangle(
                -DUAL_ELEMENT_HALF_WIDTH + pos,
                -DUAL_ELEMENT_HALF_HEIGHT,
                DUAL_ELEMENT_HALF_WIDTH,
                DUAL_ELEMENT_HEIGHT,
                ColorPalette.elementPrimary,
                radius = 5f
            )

            text(left, DUAL_LEFT_TEXT_POSITION, 0f, ColorPalette.text.darkerIf(leftIsHovered), 12f, align = TextAlign.Middle)
            text(right, DUAL_RIGHT_TEXT_POSITION, 0f, ColorPalette.text.darkerIf(rightIsHovered), 12f, align = TextAlign.Middle)

            GlStateManager.popMatrix()
        }

    }
    override val elementValueChangeListeners = mutableListOf<(Boolean) -> Unit>()

    override fun draw() {
        drawName()
        drawDualElement(left, right, isLeftHovered, isRightHovered, elementValue, x + elementDrawLocation, y + halfHeight, 1f, 1f, posAnim)
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton != 0) return false
        if (isLeftHovered && elementValue) {
            if (posAnim.start()) setValue(false)
            return true
        } else if (isRightHovered && !elementValue) {
            if (posAnim.start()) setValue(true)
            return true
        }
        return false
    }

}