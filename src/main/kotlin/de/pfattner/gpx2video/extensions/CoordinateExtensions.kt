package de.pfattner.gpx2video.extensions

private const val DEGREES_TO_MICRO_DEGREES_FACTOR = 1.0e6

fun Int.microDegreesToDegrees(): Double {
    return this / DEGREES_TO_MICRO_DEGREES_FACTOR
}

fun Double.degreesToMicroDegrees(): Int {
    return (this * DEGREES_TO_MICRO_DEGREES_FACTOR).toInt()
}
