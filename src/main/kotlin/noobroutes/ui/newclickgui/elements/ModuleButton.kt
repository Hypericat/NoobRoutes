package noobroutes.ui.newclickgui.elements

import net.minecraft.client.renderer.GlStateManager
import noobroutes.features.Module
import noobroutes.features.settings.impl.ActionSetting
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.features.settings.impl.DropdownSetting
import noobroutes.features.settings.impl.DualSetting
import noobroutes.features.settings.impl.HudSetting
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.features.settings.impl.SelectorSetting
import noobroutes.features.settings.impl.StringSetting
import noobroutes.font.FontRenderer
import noobroutes.ui.ColorPalette
import noobroutes.ui.ColorPalette.clickGUIColor
import noobroutes.ui.clickgui.elements.Element
import noobroutes.ui.clickgui.elements.menu.ElementAction
import noobroutes.ui.clickgui.elements.menu.ElementCheckBox
import noobroutes.ui.clickgui.elements.menu.ElementColor
import noobroutes.ui.clickgui.elements.menu.ElementDropdown
import noobroutes.ui.clickgui.elements.menu.ElementDual
import noobroutes.ui.clickgui.elements.menu.ElementHud
import noobroutes.ui.clickgui.elements.menu.ElementKeyBind
import noobroutes.ui.clickgui.elements.menu.ElementSelector
import noobroutes.ui.clickgui.elements.menu.ElementSlider
import noobroutes.ui.clickgui.elements.menu.ElementTextField
import noobroutes.ui.clickgui.util.HoverHandler
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.ui.util.animations.impl.EaseInOut
import noobroutes.utils.render.Color
import noobroutes.utils.render.ColorUtil.darkerIf
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.text

class ModuleButton(y: Float, val module: Module) : UiElement(0f, y){

    val menuElements: ArrayList<Element<*>> = ArrayList()

    init {
        //updateElements()
    }

    private val extendAnim = EaseInOut(250)
    private val hoverHandler = HoverHandler(1000, 200)

    private val colorAnim = ColorAnimation(150)
    val color: Color
        get() = colorAnim.get(clickGUIColor, Color.WHITE, module.enabled).darkerIf(isButtonHovered, 0.7f)
    var extended = false
    val width = Panel.WIDTH
    var height = 32f
        private set
    private val isButtonHovered: Boolean
        get() = isAreaHovered(-Panel.BORDER_THICKNESS, 0f, width + Panel.DOUBLE_BORDER_THICKNESS, height - 1)


    override fun draw() {
        GlStateManager.pushMatrix()
        translate(0f, y)
        roundedRectangle(0f, 0f, width, height, ColorPalette.moduleButtonColor)
        text(module.name, width * 0.5, height * 0.5, color, 14f, FontRenderer.REGULAR, TextAlign.Middle)
        GlStateManager.popMatrix()
    }






    /*
    fun updateElements() {
        var position = -1 // This looks weird, but it starts at -1 because it gets incremented before being used.
        for (setting in module.settings) {
            /** Don't show hidden settings */
            if (setting.shouldBeVisible) run addElement@{
                position++
                if (menuElements.any { it.setting === setting }) return@addElement
                val newElement = when (setting) {
                    is BooleanSetting -> ElementCheckBox(this, setting)
                    is NumberSetting -> ElementSlider(this, setting)
                    is SelectorSetting -> ElementSelector(this, setting)
                    is StringSetting -> ElementTextField(this, setting)
                    is ColorSetting -> ElementColor(this, setting)
                    is ActionSetting -> ElementAction(this, setting)
                    is DualSetting -> ElementDual(this, setting)
                    is HudSetting -> ElementHud(this, setting)
                    is KeybindSetting -> ElementKeyBind(this, setting)
                    is DropdownSetting -> ElementDropdown(this, setting)
                    else -> {
                        position--
                        return@addElement
                    }
                }
                menuElements.add(position, newElement)
            } else {
                menuElements.removeAll {
                    it.setting === setting
                }
            }
        }
    }

     */



}