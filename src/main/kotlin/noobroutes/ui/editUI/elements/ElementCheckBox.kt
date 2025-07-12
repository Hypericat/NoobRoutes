package noobroutes.ui.editUI.elements

import noobroutes.features.render.ClickGUIModule
import noobroutes.font.Font
import noobroutes.ui.clickgui.ClickGUI.TEXTOFFSET
import noobroutes.ui.clickgui.util.ColorUtil.brighter
import noobroutes.ui.clickgui.util.ColorUtil.brighterIf
import noobroutes.ui.clickgui.util.ColorUtil.buttonColor
import noobroutes.ui.clickgui.util.ColorUtil.clickGUIColor
import noobroutes.ui.clickgui.util.ColorUtil.darker
import noobroutes.ui.clickgui.util.ColorUtil.darkerIf
import noobroutes.ui.clickgui.util.ColorUtil.textColor
import noobroutes.ui.editUI.EditUI
import noobroutes.ui.editUI.Element
import noobroutes.ui.util.MouseUtils.isAreaHovered
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.ui.util.animations.impl.LinearAnimation
import noobroutes.utils.render.*

class ElementCheckBox(
    x: Float,
    y: Float,
    val width: Float,
    val height: Float,
    override val setter: (Boolean) -> Unit,
    override val getter: () -> Boolean,
    val name: String
) : Element<Boolean>(x, y) {
    private val colorAnim = ColorAnimation(250)
    private val linearAnimation = LinearAnimation<Float>(200)
    val xChange = 16
    val yChange = 12

    val isHovered: Boolean
        get() = if (!ClickGUIModule.switchType) isAreaHovered(EditUI.originX + x + width - 30f - xChange, EditUI.originY + y + 15f - 8, 21f + xChange, 24f + yChange)
        else isAreaHovered(EditUI.originX + x + width - 56f - xChange, EditUI.originY + y + 14f - 8, 47f + xChange, 20f + yChange)

    override fun draw(x: Float, y: Float) {
        text(name, x + TEXTOFFSET, y + height / 2f, textColor, 20f, Font.REGULAR)

        val color = colorAnim.get(
            clickGUIColor.darkerIf(isHovered, 0.7f),
            buttonColor.brighter(1.3f).brighterIf(isHovered, 1.3f),
            getter()
        )


        if (!ClickGUIModule.switchType) {
            //render check box
            roundedRectangle(x + width - 30f - xChange, y + 15f - 8, 21f + xChange, 24f + yChange, color, 5f)
            rectangleOutline(x + width - 30f - xChange, y + 15f - 8, 21f + xChange, 24f + yChange, clickGUIColor, 5f, 3f)
        } else {
            //render switch
            roundedRectangle(x + width - 56f - xChange, y + 14f - 8, 47f + xChange, 20f + yChange, buttonColor, 14f)

            if (getter() || linearAnimation.isAnimating()) roundedRectangle(
                x + width - 56f - xChange,
                y + 14f - 8,
                linearAnimation.get(47f + xChange, 9f, getter()),
                20f + yChange,
                color,
                14f
            )

            if (isHovered) rectangleOutline(x + width - 56f - xChange, y + 14f - 8, 47f + xChange, 20f + yChange, color.darker(.85f), 14f, 3f)
            circle(
                x + width - linearAnimation.get(42f + xChange - 12, 17f, !getter()) - 8, y + 24f - 2, 12f,
                Color(220, 220, 220).darkerIf(isHovered, 0.9f)
            )
        }
    }

    override fun mouseClicked() {
        if (isHovered) {
            if (colorAnim.start()) {
                linearAnimation.start()
               setter(!getter())
            }
        }
    }


}