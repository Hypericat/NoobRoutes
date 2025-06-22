package noobroutes.ui.editUI

import net.minecraft.util.BlockPos

abstract class Element<T>(
    var x: Float,
    var y: Float
) {
    abstract val setter: (T) -> Unit
    abstract val getter: () -> T

    abstract fun draw()

    fun updatePosition(x: Float, y: Float){
        this.x = x
        this.y = y
    }

    open fun keyTyped(typedChar: Char, keyCode: Int) {}
    open fun mouseClickedAnywhere(mouseButton: Int): Boolean {
        return false
    }
    open fun mouseReleased() {}
    open fun mouseClicked() {}

}