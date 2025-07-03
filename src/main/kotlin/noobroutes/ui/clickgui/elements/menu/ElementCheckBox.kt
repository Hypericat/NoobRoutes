package noobroutes.ui.clickgui.elements.menu

import noobroutes.features.render.ClickGUIModule
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.font.Font
import noobroutes.font.fonts.MinecraftFont
import noobroutes.ui.clickgui.ClickGUI.TEXTOFFSET
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.ui.util.animations.impl.LinearAnimation
import noobroutes.ui.clickgui.elements.Element
import noobroutes.ui.clickgui.elements.ElementType
import noobroutes.ui.clickgui.elements.ModuleButton
import noobroutes.ui.clickgui.util.ColorUtil.brighter
import noobroutes.ui.clickgui.util.ColorUtil.brighterIf
import noobroutes.ui.clickgui.util.ColorUtil.buttonColor
import noobroutes.ui.clickgui.util.ColorUtil.clickGUIColor
import noobroutes.ui.clickgui.util.ColorUtil.darker
import noobroutes.ui.clickgui.util.ColorUtil.darkerIf
import noobroutes.ui.clickgui.util.ColorUtil.elementBackground
import noobroutes.ui.clickgui.util.ColorUtil.textColor
import noobroutes.ui.clickgui.util.HoverHandler
import noobroutes.ui.util.MouseUtils.isAreaHovered
import noobroutes.utils.render.*
import noobroutes.utils.skyblock.devMessage

/**
 * Renders all the modules.
 *
 * Backend made by Aton, with some changes
 * Design mostly made by Stivais
 *
 * @author Stivais, Aton
 * @see [Element]
 */
class ElementCheckBox(parent: ModuleButton, setting: BooleanSetting) : Element<BooleanSetting>(
    parent, setting, ElementType.CHECK_BOX
) {
    private val colorAnim = ColorAnimation(250)
    private val linearAnimation = LinearAnimation<Float>(200)

    private val hover = HoverHandler(0, 150)

    override val isHovered: Boolean get() =
        if (!ClickGUIModule.switchType) isAreaHovered(x + w - 30f, y + 5f, 21f, 20f)
        else isAreaHovered(x + w - 43f, y + 4f, 34f, 20f)

        override fun draw() {
            roundedRectangle(x, y, w, h, elementBackground)
            text(name, x + TEXTOFFSET, y + h / 2f, textColor, 12f, Font.REGULAR)

            hover.handle(x + w - 43f, y + 4f, 34f, 20f)
            val color = colorAnim.get(clickGUIColor.darkerIf(hover.percent() > 0, 0.7f), buttonColor.brighter(1.3f).brighterIf(hover.percent() > 0, 1.3f), setting.enabled)


            if (!ClickGUIModule.switchType) {
                //render check box
                roundedRectangle(x + w - 30f, y + 5f, 21f, 20f, color, 5f)
                rectangleOutline(x + w - 30f, y + 5f, 21f, 20f, clickGUIColor, 5f, 3f)
            } else {
                //render switch
                roundedRectangle(x + w - 43f, y + 4f, 34f, 20f, buttonColor, 9f)

                if (linearAnimation.isAnimating()) devMessage(linearAnimation.get(34f, 9f, setting.enabled))
                if (setting.enabled || linearAnimation.isAnimating()) roundedRectangle(x + w - 43f, y + 4f, linearAnimation.get(34f, 9f, setting.enabled), 20f, color, 9f)

                if (isHovered) rectangleOutline(x + w - 43f, y + 4f, 34f, 20f, color.darker(.85f), 9f, 3f)
                circle(x + w - linearAnimation.get(33f, 17f, !setting.enabled), y + 14f, 6f,
                    Color(220, 220, 220).darkerIf(isHovered, 0.9f)
                )
            }
        }

    //240 - 43
    //197
    //240 - 33
    //207

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton == 0 && isHovered) {
            if (colorAnim.start()) {
                linearAnimation.start()
                setting.enabled = !setting.enabled
            }
            return true
        }
        return false
    }


}