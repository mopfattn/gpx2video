package de.pfattner.gpx2video.gpximporter

/**
 * A GPX track point.
 */
data class GpxTrackPoint constructor(
    var timestampInMillis: Long = 0,
    var latitudeInMicroDegrees: Int = Int.MIN_VALUE,
    var longitudeInMicroDegrees: Int = Int.MIN_VALUE,
    var altitudeInMeters: Int? = null,
)
