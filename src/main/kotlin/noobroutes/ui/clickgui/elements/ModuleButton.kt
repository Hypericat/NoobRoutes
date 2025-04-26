package com.github.wadey3636.noobroutes.ui.clickgui.elements

import com.github.wadey3636.noobroutes.features.render.ClickGUIModule
import com.github.wadey3636.noobroutes.font.OdinFont
import com.github.wadey3636.noobroutes.features.Module
import com.github.wadey3636.noobroutes.features.settings.impl.*
import com.github.wadey3636.noobroutes.ui.clickgui.ClickGUI
import com.github.wadey3636.noobroutes.ui.clickgui.Panel
import com.github.wadey3636.noobroutes.ui.clickgui.animations.impl.ColorAnimation
import com.github.wadey3636.noobroutes.ui.clickgui.animations.impl.EaseInOut
import com.github.wadey3636.noobroutes.ui.clickgui.elements.menu.*
import com.github.wadey3636.noobroutes.ui.clickgui.util.ColorUtil.brighter
import com.github.wadey3636.noobroutes.ui.clickgui.util.ColorUtil.clickGUIColor
import com.github.wadey3636.noobroutes.ui.clickgui.util.ColorUtil.darkerIf
import com.github.wadey3636.noobroutes.ui.clickgui.util.ColorUtil.moduleButtonColor
import com.github.wadey3636.noobroutes.ui.clickgui.util.HoverHandler
import com.github.wadey3636.noobroutes.ui.util.MouseUtils.isAreaHovered
import com.github.wadey3636.noobroutes.utils.render.*
import com.github.wadey3636.noobroutes.utils.render.RenderUtils.loadBufferedImage
import net.minecraft.client.renderer.texture.DynamicTexture
import kotlin.math.floor

/**
 * Renders all the modules.
 *
 * Backend made by Aton, with some changes
 * Design mostly made by Stivais
 *
 * @author Stivais, Aton
 * @see [Element]
 */
class ModuleButton(val module: Module, val panel: Panel) {

    val menuElements: ArrayList<Element<*>> = ArrayList()

    val x: Float
        inline get() = panel.x

    var y: Float = 0f
        get() = field + panel.y

    private val colorAnim = ColorAnimation(150)

    val color: Color
        get() = colorAnim.get(clickGUIColor, Color.WHITE, module.enabled).darkerIf(isButtonHovered && !isExtendButtonHovered, 0.7f)

    val width = Panel.WIDTH
    val height = 32f

    var extended = false

    private val extendAnim = EaseInOut(250)
    private val hoverHandler = HoverHandler(1000, 200)
    private val bannableIcon = DynamicTexture(loadBufferedImage("/assets/defnotstolen/clickgui/bannableIcon.png"))
    private val fpsHeavyIcon = DynamicTexture(loadBufferedImage("/assets/defnotstolen/clickgui/fpsHeavyIcon.png"))
    private val newFeatureIcon = DynamicTexture(loadBufferedImage("/assets/defnotstolen/clickgui/newFeatureIcon.png"))


    init {
        updateElements()
    }

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
                    is com.github.wadey3636.noobroutes.features.settings.impl.KeybindSetting -> ElementKeyBind(this, setting)
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

    fun draw(): Float {

        var offs = height

        hoverHandler.handle(x, y, width, height - 1)


        if (hoverHandler.percent() > 0) {
            ClickGUI.setDescription(module.description, x + width + 10f, y, hoverHandler)
        }


        roundedRectangle(x, y, width, height, moduleButtonColor)

        text(module.name, x + width / 2, y + height / 2, color, 14f, OdinFont.REGULAR, TextAlign.Middle)
        val textWidth = getTextWidth(module.name, 18f)

        if (textWidth < width - 80) {// too long text, not drawing symbol
            if (module.tag == Module.TagType.RISKY) {
                drawDynamicTexture(bannableIcon, x + width / 2 + textWidth / 2, y + 2f, 25f, 25f)
            } else if (module.tag == Module.TagType.FPSTAX) {
                drawDynamicTexture(fpsHeavyIcon, x + width / 2 + textWidth / 2, y, 35f, 35f)
            } else if (module.tag == Module.TagType.NEW && ClickGUIModule.firstTimeOnVersion) {
                drawDynamicTexture(newFeatureIcon, x + width / 2 + textWidth / 2, y, 35f, 35f)
            }
        }

        if (!extendAnim.isAnimating() && !extended || menuElements.isEmpty()) {
            drawArrow(x + width * 0.9f, y + height * 0.5f, rotation = 0f, scale = 0.9f)
            return offs
        }
        val extendedAnimPercent = extendAnim.get(0f, 1f, !extended)
        drawArrow(x + width * 0.9f, y + height * 0.5f, scale = 0.9f, rotation = extendedAnimPercent * 90f)
        var drawY = offs
        offs = height + floor(extendAnim.get(0f, getSettingHeight(), !extended))

        val scissor = scissor(x, y, width, offs)
        for (i in 0 until menuElements.size) {
            val currentY = drawY
            menuElements[i].y = currentY
            drawY += menuElements[i].render()
        }
        roundedRectangle(x, y + height, 2, drawY - height, clickGUIColor.brighter(1.65f), edgeSoftness = 0f)
        resetScissor(scissor)


        return offs
    }

    fun mouseClicked(mouseButton: Int): Boolean {
        if (isExtendButtonHovered) {
            if (extendAnim.start()) extended = !extended
            if (!extended) {
                menuElements.forEach {
                    it.listening = false
                }
            }
        }
        if (isButtonHovered && !isExtendButtonHovered) {
            if (mouseButton == 0) {
                if (colorAnim.start()) module.toggle()
                return true
            } else if (mouseButton == 1) {
                if (menuElements.size > 0) {
                    if (extendAnim.start()) extended = !extended
                    if (!extended) {
                        menuElements.forEach {
                            it.listening = false
                        }
                    }
                }
                return true
            }
        } else if (isMouseUnderButton) {
            for (i in menuElements.size - 1 downTo 0) {
                if (menuElements[i].mouseClicked(mouseButton)) {
                    updateElements()
                    return true
                }
            }
        }

        return false
    }

    fun mouseClickedAnywhere(mouseButton: Int): Boolean {
        for (i in menuElements.size - 1 downTo 0) {
            if (menuElements[i].mouseClickedAnywhere(mouseButton)) {
                updateElements()
                return true
            }
        }
        return false
    }


    fun mouseReleased(state: Int) {
        if (extended) {
            for (i in menuElements.size - 1 downTo 0) {
                menuElements[i].mouseReleased(state)
            }
        }
    }

    fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        if (extended) {
            for (i in menuElements.size - 1 downTo 0) {
                if (menuElements[i].keyTyped(typedChar, keyCode)) return true
            }
        }
        return false
    }

    private val isButtonHovered: Boolean
        get() = isAreaHovered(x, y, width, height - 1)

    private val isExtendButtonHovered: Boolean
        get() = isAreaHovered(x + width * 0.9f - 5,  y, width * 0.1f, height - 1)

    private val isMouseUnderButton: Boolean
        get() = extended && isAreaHovered(x, y + height, width)

    private fun getSettingHeight(): Float {
        var totalHeight = 0f
        for (i in menuElements) totalHeight += i.h
        return totalHeight
    }
}