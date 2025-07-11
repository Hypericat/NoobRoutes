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

    private inline val isRightHovered: Boolean
        get() = MouseUtils.isAreaHovered(x + w * 0.5f + 5f, y + 2f, w * 0.5f - 10f, 30f)

    private inline val isLeftHovered: Boolean
        get() = MouseUtils.isAreaHovered(x + 5f, y + 2f, w * 0.5f - 10f, 30f)


    companion object {
        const val DUAL_ELEMENT_WIDTH = 240f
        const val DUAL_ELEMENT_HEIGHT = 28f
        const val DUAL_ELEMENT_HALF_WIDTH = DUAL_ELEMENT_WIDTH * 0.5f
        const val DUAL_ELEMENT_HALF_HEIGHT = DUAL_ELEMENT_HEIGHT * 0.5f
        const val DUAL_LEFT_TEXT_POSITION = DUAL_ELEMENT_HALF_WIDTH / 3
        const val DUAL_RIGHT_TEXT_POSITION = DUAL_ELEMENT_HALF_WIDTH / 3 * 2


        fun drawDualElement(left: String, right: String, leftIsHovered: Boolean, rightIsHovered: Boolean, enabled: Boolean, x: Float, y: Float, xScale: Float, yScale: Float, posAnim: Animation<Float>) {
            GlStateManager.pushMatrix()
            GlStateManager.translate(x, y, 1f)
            GlStateManager.scale(xScale, yScale, 1f)
            roundedRectangle(-DUAL_ELEMENT_HALF_WIDTH, -DUAL_ELEMENT_HALF_HEIGHT, DUAL_ELEMENT_WIDTH, DUAL_ELEMENT_HEIGHT, ColorPalette.elementPrimary, 5f)

            val pos = posAnim.get(8f, DUAL_ELEMENT_HALF_WIDTH, !enabled)
            roundedRectangle(pos - DUAL_ELEMENT_HALF_WIDTH, -DUAL_ELEMENT_HALF_HEIGHT, DUAL_ELEMENT_HALF_WIDTH, DUAL_ELEMENT_HEIGHT,
                ColorPalette.elementSecondary.darker(0.8f), 5f)

            text(left, DUAL_LEFT_TEXT_POSITION, DUAL_ELEMENT_HALF_HEIGHT, Color.WHITE.darkerIf(leftIsHovered), 12f, Font.REGULAR, TextAlign.Middle)
            text(right, DUAL_RIGHT_TEXT_POSITION,DUAL_ELEMENT_HALF_HEIGHT, Color.WHITE.darkerIf(rightIsHovered), 12f, Font.REGULAR, TextAlign.Middle)
            GlStateManager.popMatrix()
        }

    }


    override val elementValueChangeListeners = mutableListOf<(Boolean) -> Unit>()

    override fun draw() {
        drawName()
        drawDualElement(left, right, isLeftHovered, isRightHovered, elementValue, x + w * 0.6f, y + halfHeight, 1f, 1f, posAnim)
    }
}