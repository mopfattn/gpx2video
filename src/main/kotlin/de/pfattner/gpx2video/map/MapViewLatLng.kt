package de.pfattner.gpx2video.map

import de.pfattner.gpx2video.extensions.microDegreesToDegrees
import de.pfattner.gpx2video.gpximporter.GpxTrackPoint

/**
 * A coordinate used by a [MapViewPolyline].
 */
data class MapViewLatLng(
    val latitude: Double,
    val longitude: Double
) {
    constructor(trackPoint: GpxTrackPoint) : this(
        trackPoint.latitudeInMicroDegrees.microDegreesToDegrees(),
        trackPoint.longitudeInMicroDegrees.microDegreesToDegrees()
    )
}