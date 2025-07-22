package noobroutes.ui.util.animations.impl

import noobroutes.ui.util.animations.Animation
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class CubicBezierAnimation(duration: Long, val x1: Float, val y1: Float, val x2: Float, val y2: Float, val doCaching: Boolean = true): Animation<Float>(duration) {
    class CubicBezierCache(val y: Float, val dx: Float)

    companion object {
        const val INTERPOLATION_COUNT: Int = 60
        const val APPROXIMATION_ITERATIONS: Int = 5

        const val CALCULATION_INTERPOLATION_COUNT: Float = INTERPOLATION_COUNT - 1f
    }
    private val cache = mutableMapOf<Int, CubicBezierCache>()
    constructor(duration: Long, x1: Number, y1: Number, x2: Number, y2: Number, doCaching: Boolean = true) : this(duration, x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat(), doCaching)
    init {
        if (doCaching) {
            for (i in 0..INTERPOLATION_COUNT) {
                val x = i / INTERPOLATION_COUNT.toFloat()
                val t = getT(x)
                val y = bezier(t, 0f, y1, y2, 1f)
                val dx = getDxFromT(t)
                cache[i] = CubicBezierCache(y, dx)
            }
        }
    }
    override fun get(start: Float, end: Float, reverse: Boolean): Float {
        if (!isAnimating()) return if (reverse) start else end

        val progress = if (reverse) end + (start - end) else start + (end - start)
        val x = getPercent() * 0.01f
        if (doCaching) {
            val selector = (x * CALCULATION_INTERPOLATION_COUNT).toInt().coerceIn(0, INTERPOLATION_COUNT)
            val interpolationProgress = x - (selector / INTERPOLATION_COUNT.toFloat())
            val c = cache[selector]
                ?: throw RuntimeException("Castrophic failure, failed to get cache at selector $selector, $cache")
            return (c.y + c.dx * interpolationProgress) * progress
        }
        else {
            return progress * getCubicBezier(x)
        }
    }

    private fun bezier(t: Float, p0: Float, p1: Float, p2: Float, p3: Float): Float {
        val u = 1 - t
        return (u * u * u * p0 + 3 * u * u * t * p1 + 3 * u * t * t * p2 + t * t * t * p3)
    }

    private fun bezierDerivative(t: Float, p0: Float, p1: Float, p2: Float, p3: Float): Float {
        val u = 1 - t
        return (3 * u * u * (p1 - p0) + 6 * u * t * (p2 - p1) + 3 * t * t * (p3 - p2))
    }

    private fun getT(x: Float): Float {
        var t = x
        for (i in 1..APPROXIMATION_ITERATIONS) {
            val xEstimate = bezier(t, 0f, x1, x2, 1f)
            val dx = bezierDerivative(t, 0f, x1, x2, 1f)
            if (abs(dx) < 1e-6) break
            val tNext = t - (xEstimate - x) / dx
            if (abs(tNext - t) < 1e-6) break;
            t = tNext
        }
        return max(0f, min(1f, t))
    }

    private fun getDxFromT(t: Float): Float {
        val dx = bezierDerivative(t, 0f, x1, x2, 1f)
        val dy = bezierDerivative(t, 0f, y1, y2, 1f)
        return if (dx == 0f) 0f else dy / dx
    }

    private fun getCubicBezier(x: Float): Float {
        return bezier(getT(x), 0f, y1, y2, 1f);
    }
}