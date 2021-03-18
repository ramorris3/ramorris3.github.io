package dev.littlegames.highorderfunctions

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun approach(start: Float, end: Float, inc: Float) : Float {
    // going down
    if (start > end) {
        return max(end, start - abs(inc))
    }
    // going up
    return min(end, start + abs(inc))
}
