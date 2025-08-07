package noobroutes.ui.util

import noobroutes.Core.mc
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.Display

/**
 * Taken from Odin
 */
object MouseUtils {

    val mouseX: Float
        get() = Mouse.getX().toFloat()

    val mouseY: Float
        get() = mc.displayHeight - Mouse.getY() - 1f

    fun isAreaHovered(x: Float, y: Float, w: Float, h: Float): Boolean {
        return mouseX in x..x + w && mouseY in y..y + h
    }

    fun isAreaHovered(x: Float, y: Float, w: Float): Boolean {
        return mouseX in x..x + w && mouseY >= y
    }

    fun getQuadrant(): Int {
        return when {
            mouseX >= Display.getWidth() * 0.5 -> if (mouseY >= Display.getHeight() * 0.5) 4 else 2
            else -> if (mouseY >= Display.getHeight() * 0.5) 3 else 1
        }
    }
}