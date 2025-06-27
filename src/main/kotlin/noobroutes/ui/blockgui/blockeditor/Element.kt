package noobroutes.ui.blockgui.blockeditor

abstract class Element(
    var x: Float,
    var y: Float
) {
    abstract fun draw()
    abstract fun getElementHeight(): Float


    open fun keyTyped(typedChar: Char, keyCode: Int) {}
    open fun mouseClickedAnywhere(mouseButton: Int): Boolean {
        return false
    }
    open fun mouseReleased() {}
    open fun mouseClicked() {}

}