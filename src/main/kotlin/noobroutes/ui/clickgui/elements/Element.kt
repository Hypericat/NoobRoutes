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
 * @see [Element]
 */
open class Element<S : Setting<*>>(val setting: S, val type: ElementType): UiElement(0f, 0f) {

    inline val name: String
        get () = setting.name

    val w: Float = Panel.WIDTH

    var h: Float = when (type) {
        ElementType.SLIDER -> 55f
        else -> DEFAULT_HEIGHT
    }

    var extended = false
    var listening = false


    open val isHovered
        get() = isAreaHovered(x, y, w, h)

    open fun getHeight(): Float {
        return if (visible) h else 0f
    }

    companion object {
        const val DEFAULT_HEIGHT = 32f
        const val BORDER_OFFSET = 9f
    }
}