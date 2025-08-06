package noobroutes.ui.clickgui.elements

import net.minecraft.client.renderer.GlStateManager
import noobroutes.Core
import noobroutes.Core.logger
import noobroutes.features.Module
import noobroutes.features.settings.Setting
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
import noobroutes.ui.clickgui.elements.menu.ElementAction
import noobroutes.ui.clickgui.elements.menu.ElementSwitch
import noobroutes.ui.clickgui.elements.menu.ElementColor
import noobroutes.ui.clickgui.elements.menu.ElementDropdown
import noobroutes.ui.clickgui.elements.menu.ElementDual
import noobroutes.ui.clickgui.elements.menu.ElementHud
import noobroutes.ui.clickgui.elements.menu.ElementKeyBind
import noobroutes.ui.clickgui.elements.menu.ElementSelector
import noobroutes.ui.clickgui.elements.menu.ElementSlider
import noobroutes.ui.clickgui.elements.menu.ElementTextField
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.ui.util.animations.impl.CubicBezierAnimation
import noobroutes.ui.util.animations.impl.EaseInOut
import noobroutes.utils.render.Color
import noobroutes.utils.render.ColorUtil.brighter
import noobroutes.utils.render.ColorUtil.darkerIf
import noobroutes.utils.render.ColorUtil.hsbMax
import noobroutes.utils.render.ColorUtil.withAlpha
import noobroutes.utils.render.TextAlign
import noobroutes.utils.render.popStencil
import noobroutes.utils.render.resetScissor
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.scissor
import noobroutes.utils.render.stencilRoundedRectangle
import noobroutes.utils.render.text
import org.lwjgl.input.Keyboard
import kotlin.math.floor

class  ModuleButton(y: Float, val module: Module) : UiElement(0f, y){
    companion object {
        const val BUTTON_HEIGHT = 32f
    }

    init {
        updateElements()
    }

    private val extendAnim = CubicBezierAnimation(250, 0.4, 0, 0.2, 1)


    private inline val UiElement.element get() = (this as Element<*>)

    private val colorAnim = ColorAnimation(150)
    val color: Color
        get() = colorAnim.get(clickGUIColor, Color.WHITE, module.enabled).darkerIf(isButtonHovered, 0.7f)
    var extended = false
    val width = Panel.WIDTH

    private val isButtonHovered: Boolean
        get() = isAreaHovered(-Panel.BORDER_THICKNESS, 0f, width + Panel.DOUBLE_BORDER_THICKNESS, BUTTON_HEIGHT - 1)

    fun getHeight(): Float {
        return BUTTON_HEIGHT + floor(extendAnim.get(0f, getOptionsHeight(), !extended))
    }

    private fun getOptionsHeight(): Float {
        var drawY = 0f
        for (i in 0 until uiChildren.size) {
            drawY += uiChildren[i].element.getHeight()
        }
        return drawY
    }

    override fun doHandleDraw() {
        if (!visible) return
        GlStateManager.pushMatrix()
        translate(0f, y)
        roundedRectangle(0f, 0f, width, BUTTON_HEIGHT, ColorPalette.moduleButtonColor)
        text(module.name, width * 0.5, BUTTON_HEIGHT * 0.5, color, 14f, FontRenderer.REGULAR, TextAlign.Middle)


        if (!extendAnim.isAnimating() && !extended) {

            for (i in uiChildren.indices) {
                uiChildren[i].visible = false
            }
            GlStateManager.popMatrix()
            return
        }

        var drawY = BUTTON_HEIGHT
        for (i in uiChildren.indices) {
            uiChildren[i].apply {
                visible = true
                updatePosition(0f, drawY)
                drawY += (this as Element<*>).getHeight()
            }
        }

        val scissor = scissor(x + getEffectiveX() - 3f, BUTTON_HEIGHT + getEffectiveY(), width * getEffectiveXScale() + 3, (drawY - BUTTON_HEIGHT) * extendAnim.get(0f, 1f, !extended) * getEffectiveYScale())
        doDrawChildren()
        roundedRectangle(x, BUTTON_HEIGHT, 2, drawY - BUTTON_HEIGHT, clickGUIColor.brighter(1.65f), edgeSoftness = 0f)

        resetScissor(scissor)
        GlStateManager.popMatrix()
    }

    override fun mouseClicked(mouseButton: Int): Boolean {
        if (!isButtonHovered) return false
        if (mouseButton == 0) {
            if (colorAnim.start()) module.toggle()
            return true
        }
        if (mouseButton == 1) {
            if (uiChildren.isEmpty()) return true
            if (extendAnim.start()) extended = !extended
            return true
        }
        return true
    }

    fun updateElements() {
        uiChildren.clear()
        for (setting in module.settings) {
            if (setting.shouldBeVisible) run addElement@{
                if (uiChildren.any { it.element.setting === setting }) return@addElement
                if (setting.devOnly && !Core.DEV_MODE) {
                    setting.reset()
                    if (setting is KeybindSetting) {
                        setting.value.key = Keyboard.KEY_NONE
                    }
                    return@addElement
                }

                val newElement = when (setting) {
                    is BooleanSetting -> ElementSwitch(setting)
                    is NumberSetting -> ElementSlider(setting)
                    is SelectorSetting -> ElementSelector(setting)
                    is StringSetting -> ElementTextField(setting)
                    is ColorSetting -> ElementColor(setting)
                    is ActionSetting -> ElementAction(setting)
                    is DualSetting -> ElementDual(setting)
                    is HudSetting -> ElementHud(setting)
                    is KeybindSetting -> ElementKeyBind(setting)
                    is DropdownSetting -> ElementDropdown(setting)
                    else -> return@addElement
                }
                addChild(newElement)
            }
        }
    }
}