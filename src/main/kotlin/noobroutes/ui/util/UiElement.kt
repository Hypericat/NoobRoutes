package noobroutes.ui.util

import noobroutes.ui.ColorPalette
import noobroutes.ui.clickgui.util.ColorUtil.darker
import noobroutes.utils.render.Color
import noobroutes.utils.render.roundedRectangle


abstract class UiElement<T> (var x: Float, var y: Float, val w: Float, val h: Float) {
    private val elementValueChangeListeners = mutableListOf<(T) -> Unit>()
    protected abstract var elementValue: T

    protected val halfHeight = h * 0.5f
    protected val halfWidth = w * 0.5f

    protected fun drawBackground(){
        roundedRectangle(x, y, w, h, ColorPalette.background.darker(0.6f), radius = 15f)

    }

    fun changePosition(x: Float, y: Float){
        this.x = x
        this.y = y
    }

    fun addValueChangeListener(listener: (T) -> Unit){
        elementValueChangeListeners.add(listener)
    }

    abstract fun draw()

    fun setValue(value: T) {
        this.elementValue = value
        for (listener in elementValueChangeListeners) {
            listener.invoke(elementValue)
        }
    }
    fun getValue(): T {
        return elementValue
    }

    open fun mouseLeftClicked() {}
    open fun mouseReleased() {}
    open fun mouseClickedAnywhere() {}
    open fun keyTyped(typedChar: Char, keyCode: Int) {}
}