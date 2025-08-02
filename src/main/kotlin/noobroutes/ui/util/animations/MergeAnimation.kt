package noobroutes.ui.util.animations


class MergeAnimation<T>(val startAnimation: Animation<T>, val endAnimation: Animation<T>) {

    private inline val animating get() = startAnimation.isAnimating() || endAnimation.isAnimating()
    private var currentAnimationState = 0
    private val currentAnimation get() = if (currentAnimationState == 0) startAnimation else endAnimation

    fun start(reverse: Boolean, bypass: Boolean = false): Boolean {
        if (!animating || bypass) {
            if (reverse) {
                endAnimation.start(bypass)
                currentAnimationState = 1
                return true
            }
            startAnimation.start(bypass)
            currentAnimationState = 0
            return true
        }
        return false
    }

    fun getPercent(): Int {
        return if (animating) {
            return currentAnimation.getPercent()
        } else {
            100
        }
    }

    fun isAnimating(): Boolean {
        return animating
    }

    fun get(start: T, end: T): T {
        return currentAnimation.get(start, end, currentAnimationState == 1)
    }


}