package noobroutes.ui.clickgui.elements

import noobroutes.features.settings.Setting
import noobroutes.ui.util.UiElement

/**
 * Renders all the modules.
 *
 * Backend made by Aton, with some changes
 * Design mostly made by Stivais
 *
 * @author Stivais, Aton
 * @see [SettingElement]
 */
open class SettingElement<S : Setting<*>>(val setting: S, val type: ElementType): UiElement(0f, 0f) {

    inline val name: String
        get () = setting.name

    val w: Float = Panel.WIDTH

    var h: Float = when (type) {
        ElementType.SLIDER -> 55f
        else -> DEFAULT_HEIGHT
    }

    var extended = false

    val isDescriptionHovered get() = isAreaHovered(0f, 0f, w, h)


    var lastSettingHovered = -1L


    private fun handleDescription(){
        if (!visible) return
        if (!isDescriptionHovered) {
            if (lastSettingHovered != -1L) {
                ClickGUIBase.wipeDescription()
            }
            lastSettingHovered = -1L
            return
        }
        if (lastSettingHovered == -1L) lastSettingHovered = System.currentTimeMillis()

        if (System.currentTimeMillis() - lastSettingHovered > 1000L) {
            ClickGUIBase.setDescription(setting.description, getEffectiveX(), getEffectiveY())
        }
    }

    override fun doHandleDraw() {
        handleDescription()
        super.doHandleDraw()
    }

    open val isHovered
        get() = isAreaHovered(0f, 0f, w, h)

    open fun getHeight(): Float {
        return if (visible) h else 0f
    }

    companion object {
        const val DEFAULT_HEIGHT = 32f
        const val BORDER_OFFSET = 9f
    }
}