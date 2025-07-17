package noobroutes.ui.util


abstract class UiElement(var x: Float, var y: Float) {



    fun updatePosition(x: Float, y: Float){
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