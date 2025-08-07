package noobroutes.ui.util.animations

import noobroutes.utils.clock.Clock

/**
 * Simple class that calculates a "point" between two values and a percentage.
 * @author Stivais
 */
abstract class Animation<T>(private var duration: Long) {

    private var animating = false
    private val clock = Clock(duration)

    fun start(bypass: Boolean = false, percentage: Int = 0): Boolean {
        if (animating) {
            updateIsAnimating()
        }
        if (!animating || bypass) {
            animating = true
            clock.setTime(System.currentTimeMillis() - (duration * percentage * 0.01).toLong())
            return true
        }
        return false
    }

    fun getPercent(): Int {
        return if (animating) {
            val percent = (clock.getTime() / duration.toDouble() * 100).toInt().coerceAtMost(100)
            if (percent == 100) animating = false
            percent
        } else {
            100
        }
    }

    fun isAnimating(): Boolean {
        if (animating) {
            updateIsAnimating()
            return animating
        }
        return false
    }
    fun updateIsAnimating(){
        if (clock.getTime() >= duration) {
            animating = false
        }
    }

    abstract fun get(start: T, end: T, reverse: Boolean = false): T
}