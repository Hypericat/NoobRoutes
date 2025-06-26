package noobroutes.ui.blockgui

abstract class Element<T>(
    val x: Float,
    val y: Float
) {
    abstract val setter: (T) -> Unit
    abstract val getter: () -> T

    abstract fun draw(x: Float, y: Float)



    open fun keyTyped(typedChar: Char, keyCode: Int) {}
    open fun mouseClickedAnywhere(mouseButton: Int): Boolean {
        return false
    }
    open fun mouseReleased() {}
    open fun mouseClicked() {}

}