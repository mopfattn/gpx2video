package de.pfattner.gpx2video.gpximporter

/**
 * A GPX track segment containing a list of track points.
 */
data class GpxTrackSegment(
    val trackPoints: MutableList<GpxTrackPoint> = mutableListOf()
)
