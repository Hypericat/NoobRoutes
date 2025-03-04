package com.github.wadey3636.noobroutes.utils

import kotlin.math.cos
import kotlin.math.sin

object Utils {

    fun xPart(yaw: Double): Double {
        return -sin(yaw * Math.PI /180)
    }

    fun zPart(yaw: Double): Double {
        return cos(yaw * Math.PI /180)
    }
}