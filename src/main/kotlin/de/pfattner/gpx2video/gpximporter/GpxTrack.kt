package de.pfattner.gpx2video.gpximporter

/**
 * A GPX track containing a list of track segments.
 */
data class GpxTrack(
    var name: String? = null,
    var trackSegments: MutableList<GpxTrackSegment> = mutableListOf()
)
