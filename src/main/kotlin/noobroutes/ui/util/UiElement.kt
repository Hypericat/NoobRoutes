package noobroutes.ui.util


abstract class UiElement(var x: Float, var y: Float) {
    val uiChildren = mutableListOf<UiElement>()

    fun updatePosition(x: Float, y: Float){
        val deltaX = this.x - x
        val deltaY = this.y - y
        this.x = x
        this.y = y
        uiChildren.forEach { it.updatePosition(it.x + deltaX, y + deltaY) }
    }

    open fun draw() {
        uiChildren.forEach { it.draw() }
    }

    open fun mouseClicked(mouseButton: Int): Boolean {
        return uiChildren.any { it.mouseClicked(mouseButton) }
    }
    open fun mouseReleased(): Boolean {
        return uiChildren.any { it.mouseReleased() }
    }

    open fun mouseClickedAnywhere() {
        uiChildren.forEach { it.mouseClickedAnywhere() }
    }
    open fun keyTyped(typedChar: Char, keyCode: Int): Boolean {
        return uiChildren.any { it.keyTyped(typedChar, keyCode) }
    }

}