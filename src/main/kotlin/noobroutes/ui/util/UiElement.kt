package noobroutes.ui.util

import noobroutes.font.Font
import noobroutes.ui.ColorPalette
import noobroutes.ui.ColorPalette.TEXT_OFFSET
import noobroutes.ui.clickgui.util.ColorUtil.darker
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.text


abstract class UiElement(val name: String, var x: Float, var y: Float, val w: Float, val h: Float) {

    protected val halfHeight = h * 0.5f
    protected val halfWidth = w * 0.5f

    protected fun drawName(){
        text(
            name,
            x + TEXT_OFFSET,
            y + h * 0.5f,
            ColorPalette.text,
            16f,
            fontType = ColorPalette.font,
            type = Font.REGULAR
        )
    }

    protected fun drawBackground(){
        roundedRectangle(x, y, w, h, ColorPalette.backgroundPrimary.darker(0.6f), radius = 15f)
    }

    fun changePosition(x: Float, y: Float){
        this.x = x
        this.y = y
    }

    abstract fun draw()


    open fun mouseClicked(mouseButton: Int): Boolean {
        return false
    }
    open fun mouseReleased(): Boolean {
        return false
    }
    open fun mouseClickedAnywhere() {}
    open fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        return false
    }
}