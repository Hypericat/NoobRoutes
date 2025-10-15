package noobroutes.ui.clickgui.elements

import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.texture.DynamicTexture
import noobroutes.Core
import noobroutes.features.Module
import noobroutes.features.settings.impl.*
import noobroutes.font.FontRenderer
import noobroutes.ui.ColorPalette
import noobroutes.ui.ColorPalette.TEXT_OFFSET
import noobroutes.ui.ColorPalette.clickGUIColor
import noobroutes.ui.clickgui.elements.menu.*
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.ColorAnimation
import noobroutes.ui.util.animations.impl.CubicBezierAnimation
import noobroutes.utils.render.*
import noobroutes.utils.render.ColorUtil.brighter
import noobroutes.utils.render.ColorUtil.darkerIf
import noobroutes.utils.render.RenderUtils.loadBufferedImage
import org.lwjgl.input.Keyboard
import kotlin.math.floor

class  ModuleButton(y: Float, val module: Module) : UiElement(0f, y){
    companion object {
        const val BUTTON_HEIGHT = 32f
        const val HALF_BUTTON_HEIGHT = BUTTON_HEIGHT * 0.5f
        val warningIcon = DynamicTexture(loadBufferedImage("/assets/ui/WarningIcon.png"))
    }

    init {
        updateElements()
    }

    private val extendAnim = CubicBezierAnimation(250, 0.4, 0, 0.2, 1)


    private inline val UiElement.settingElement get() = (this as SettingElement<*>)

    private val colorAnim = ColorAnimation(150)
    val color: Color
        get() = colorAnim.get(clickGUIColor, Color.WHITE, module.enabled).darkerIf(isButtonHovered, 0.7f)
    var extended = false
    val width = Panel.WIDTH

    var lastButtonHovered = -1L

    private val isButtonHovered: Boolean
        get() = isAreaHovered(-Panel.BORDER_THICKNESS, 0f, width + Panel.DOUBLE_BORDER_THICKNESS, BUTTON_HEIGHT - 1)

    fun getHeight(): Float {
        return BUTTON_HEIGHT + floor(extendAnim.get(0f, getOptionsHeight(), !extended))
    }

    private fun getOptionsHeight(): Float {
        var drawY = 0f
        for (child in uiChildren) {
            val setting = child.settingElement
            if (!setting.setting.shouldBeVisible) continue
            drawY += setting.getHeight()
        }
        return drawY
    }

    private fun handleDescription() {
        if (!isButtonHovered) {
            if (lastButtonHovered != -1L) {
                ClickGUIBase.wipeDescription()
            }
            lastButtonHovered = -1L
            return
        }
        if (lastButtonHovered == -1L) lastButtonHovered = System.currentTimeMillis()

        if (System.currentTimeMillis() - lastButtonHovered > 1000L) {
            ClickGUIBase.setDescription(module.description, getEffectiveX(), getEffectiveY())
        }
    }

    override fun doHandleDraw() {
        if (!visible) return
        handleDescription()
        GlStateManager.pushMatrix()
        translate(0f, y)
        roundedRectangle(0f, 0f, width, BUTTON_HEIGHT, ColorPalette.moduleButtonColor)
        text(module.name, width * 0.5, HALF_BUTTON_HEIGHT, color, 14f, FontRenderer.REGULAR, TextAlign.Middle)

        if (module.warning) drawDynamicTexture(warningIcon, TEXT_OFFSET, 0f, 28f, 28f)


        if (!extendAnim.isAnimating() && !extended) {

            for (i in uiChildren.indices) {
                uiChildren[i].visible = false
            }
            GlStateManager.popMatrix()
            return
        }

        var drawY = BUTTON_HEIGHT
        for (child in uiChildren) {
            val setting = child as SettingElement<*>
            if (!setting.setting.shouldBeVisible) {
                setting.visible = false
                continue
            }
            setting.visible = true
            setting.updatePosition(0f, drawY)
            drawY += setting.getHeight()
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
            run addElement@{
                if (uiChildren.any { it.settingElement.setting === setting }) return@addElement
                if (setting.devOnly && !Core.DEV_MODE) {
                    setting.reset()
                    if (setting is KeybindSetting) {
                        setting.value.key = Keyboard.KEY_NONE
                    }
                    return@addElement
                }

                val newElement = when (setting) {
                    is BooleanSetting -> SettingElementSwitch(setting)
                    is NumberSetting -> SettingElementSlider(setting)
                    is SelectorSetting -> SettingElementSelector(setting)
                    is DynamicListSetting -> SettingElementDynamicList(setting)
                    is StringSetting -> SettingElementTextField(setting)
                    is ColorSetting -> SettingElementColor(setting)
                    is ActionSetting -> SettingElementAction(setting)
                    is DualSetting -> SettingElementDual(setting)
                    is HudSetting -> SettingElementHud(setting)
                    is KeybindSetting -> SettingElementKeyBind(setting)
                    is DropdownSetting -> SettingElementDropdown(setting)
                    else -> return@addElement
                }
                addChild(newElement)
            }
        }
    }
}