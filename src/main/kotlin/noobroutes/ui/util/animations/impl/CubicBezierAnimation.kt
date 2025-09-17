package noobroutes.ui.util.animations.impl

import noobroutes.ui.util.animations.Animation
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CubicBezierAnimation(duration: Long, val x1: Float, val y1: Float, val x2: Float, val y2: Float): Animation<Float>(duration) {

    companion object {
        const val INTERPOLATION_COUNT: Int = 60
        const val APPROXIMATION_ITERATIONS: Int = 5
        const val CALCULATION_INTERPOLATION_COUNT: Float = INTERPOLATION_COUNT - 1f
        const val EPSILON: Float = 1e-6f

        private fun hashBezierParams(x1: Float, y1: Float, x2: Float, y2: Float): Int {
            var result = x1.toBits()
            result = 31 * result + y1.toBits()
            result = 31 * result + x2.toBits()
            result = 31 * result + y2.toBits()
            return result
        }

        private val globalCache = ConcurrentHashMap<Int, Map<Int, Float>>()
    }

    constructor(duration: Long, x1: Number, y1: Number, x2: Number, y2: Number) : this(duration, x1.toFloat(), y1.toFloat(), x2.toFloat(), y2.toFloat())

    private val cache: Map<Int, Float>

    init {
        val hash = hashBezierParams(x1, y1, x2, y2)

        this.cache = globalCache.getOrPut(hash) {
            val newCache = mutableMapOf<Int, Float>()
            for (i in 0..INTERPOLATION_COUNT) {
                val x = i / INTERPOLATION_COUNT.toFloat()
                val t = getT(x)
                val y = bezier(t, 0f, y1, y2, 1f)
                newCache[i] = y

            }
            newCache
        }

    }

    override fun get(start: Float, end: Float, reverse: Boolean): Float {
        if (!isAnimating()) return if (reverse) start else end

        val progress = getPercent() * 0.01f
        val x = if (reverse) 1f - progress else progress


        val selector = (x * CALCULATION_INTERPOLATION_COUNT).toInt().coerceIn(0, INTERPOLATION_COUNT - 1)
        val interpolationProgress = (x * CALCULATION_INTERPOLATION_COUNT) - selector

        val currentCache = cache[selector] ?: throw RuntimeException("Failed to get cache at selector $selector")

        val nextCache = if (selector < INTERPOLATION_COUNT - 1) {
            cache[selector + 1] ?: currentCache
        } else {
            currentCache
        }

        val animationValue = currentCache + (nextCache - currentCache) * interpolationProgress



        return start + (end - start) * animationValue
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
            if (abs(dx) < EPSILON) break
            val tNext = t - (xEstimate - x) / dx
            if (abs(tNext - t) < EPSILON) break
            t = tNext
        }
        return max(0f, min(1f, t))
    }
}