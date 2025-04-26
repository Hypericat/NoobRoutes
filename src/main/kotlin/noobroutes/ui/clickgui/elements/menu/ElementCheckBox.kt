package com.github.wadey3636.noobroutes.ui.clickgui.elements.menu

import com.github.wadey3636.noobroutes.features.render.ClickGUIModule
import com.github.wadey3636.noobroutes.font.OdinFont
import com.github.wadey3636.noobroutes.features.settings.impl.BooleanSetting
import com.github.wadey3636.noobroutes.ui.clickgui.ClickGUI.TEXTOFFSET
import com.github.wadey3636.noobroutes.ui.clickgui.animations.impl.ColorAnimation
import com.github.wadey3636.noobroutes.ui.clickgui.animations.impl.LinearAnimation
import com.github.wadey3636.noobroutes.ui.clickgui.elements.Element
import com.github.wadey3636.noobroutes.ui.clickgui.elements.ElementType
import com.github.wadey3636.noobroutes.ui.clickgui.elements.ModuleButton
import com.github.wadey3636.noobroutes.ui.clickgui.util.ColorUtil.brighter
import com.github.wadey3636.noobroutes.ui.clickgui.util.ColorUtil.brighterIf
import com.github.wadey3636.noobroutes.ui.clickgui.util.ColorUtil.buttonColor
import com.github.wadey3636.noobroutes.ui.clickgui.util.ColorUtil.clickGUIColor
import com.github.wadey3636.noobroutes.ui.clickgui.util.ColorUtil.darker
import com.github.wadey3636.noobroutes.ui.clickgui.util.ColorUtil.darkerIf
import com.github.wadey3636.noobroutes.ui.clickgui.util.ColorUtil.elementBackground
import com.github.wadey3636.noobroutes.ui.clickgui.util.ColorUtil.textColor
import com.github.wadey3636.noobroutes.ui.clickgui.util.HoverHandler
import com.github.wadey3636.noobroutes.ui.util.MouseUtils.isAreaHovered
import com.github.wadey3636.noobroutes.utils.render.*

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
            text(name, x + TEXTOFFSET, y + h / 2f, textColor, 12f, OdinFont.REGULAR)

            hover.handle(x + w - 43f, y + 4f, 34f, 20f)
            val color = colorAnim.get(clickGUIColor.darkerIf(hover.percent() > 0, 0.7f), buttonColor.brighter(1.3f).brighterIf(hover.percent() > 0, 1.3f), setting.enabled)


            if (!ClickGUIModule.switchType) {
                //render check box
                dropShadow(x + w - 30f, y + 5f, 21f, 20f, 10f, 0.75f)
                roundedRectangle(x + w - 30f, y + 5f, 21f, 20f, color, 5f)
                rectangleOutline(x + w - 30f, y + 5f, 21f, 20f, clickGUIColor, 5f, 3f)
            } else {
                //render switch
                dropShadow(x + w - 43f, y + 4f, 34f, 20f, 10f, 0.75f)
                roundedRectangle(x + w - 43f, y + 4f, 34f, 20f, buttonColor, 9f)

                if (setting.enabled || linearAnimation.isAnimating()) roundedRectangle(x + w - 43f, y + 4f, linearAnimation.get(34f, 9f, setting.enabled), 20f, color, 9f)

                if (isHovered) rectangleOutline(x + w - 43f, y + 4f, 34f, 20f, color.darker(.85f), 9f, 3f)
                circle(x + w - linearAnimation.get(33f, 17f, !setting.enabled), y + 14f, 6f,
                    Color(220, 220, 220).darkerIf(isHovered, 0.9f)
                )
            }
        }

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